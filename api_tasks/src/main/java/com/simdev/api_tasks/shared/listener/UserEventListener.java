package com.simdev.api_tasks.shared.listener;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simdev.api_tasks.shared.dto.UserEvent;
import com.simdev.api_tasks.shared.domain.ProcessedEvent;
import com.simdev.api_tasks.shared.repository.ProcessedEventRepository;
import com.simdev.api_tasks.shared.service.UserCacheService;
import com.simdev.api_tasks.task.repository.TaskRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.DltHandler;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.annotation.RetryableTopic;
import org.springframework.kafka.retrytopic.DltStrategy;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.retry.annotation.Backoff;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Listener Kafka pour les événements utilisateurs (architecture event-driven).
 * <p>
 * Ce listener écoute le topic Kafka {@code user-events} et réagit automatiquement
 * aux changements d'état des utilisateurs pour maintenir la cohérence des données
 * entre les microservices.
 * </p>
 * <p>
 * <strong>Fonctionnalités principales :</strong>
 * <ul>
 *   <li><strong>Écoute événementielle :</strong> Réagit aux événements utilisateurs en temps réel</li>
 *   <li><strong>Gestion des tâches orphelines :</strong> Supprime automatiquement les tâches
 *       lorsque leur utilisateur propriétaire est supprimé</li>
 *   <li><strong>Invalidation du cache :</strong> Vide le cache utilisateur lors des mises à jour</li>
 *   <li><strong>Idempotence :</strong> Garantit qu'un événement n'est traité qu'une seule fois</li>
 *   <li><strong>Résilience :</strong> Retry automatique avec Dead Letter Topic (DLT)</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Système de retry :</strong>
 * <ul>
 *   <li>3 tentatives automatiques avec backoff exponentiel (1s, 2s, 4s)</li>
 *   <li>Après épuisement des retries, le message est envoyé au DLT ({@code user-events.DLT})</li>
 *   <li>Commit manuel pour garantir le traitement même en cas d'erreur</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Événements traités :</strong>
 * <ul>
 *   <li><strong>user.deleted :</strong> Supprime toutes les tâches de l'utilisateur et invalide le cache</li>
 *   <li><strong>user.updated :</strong> Invalide le cache de l'utilisateur pour forcer le rechargement</li>
 *   <li><strong>user.created :</strong> Invalide le cache (nouvel utilisateur)</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Idempotence :</strong>
 * Chaque événement est identifié par une clé unique (partition + offset). Les événements
 * déjà traités sont ignorés, garantissant qu'un même événement n'est jamais traité deux fois,
 * même en cas de retry.
 * </p>
 *
 * @author API Tasks Service
 * @version 1.0
 * @see UserCacheService
 * @see TaskRepository
 * @see ProcessedEventRepository
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class UserEventListener {
    
    private final TaskRepository taskRepository;
    private final ObjectMapper objectMapper;
    private final UserCacheService userCacheService;
    private final ProcessedEventRepository processedEventRepository;
    
    /**
     * Écoute les événements utilisateurs depuis Kafka avec retry automatique.
     * <p>
     * Utilise @RetryableTopic pour gérer automatiquement les retries :
     * - 3 tentatives avec backoff exponentiel (1s, 2s, 4s)
     * - Après échec, le message est envoyé au Dead Letter Topic
     * - Commit manuel pour garantir le traitement
     * </p>
     *
     * @param message Le message JSON de l'événement utilisateur
     * @param acknowledgment L'objet pour commit manuel
     * @param partition La partition Kafka
     * @param offset L'offset du message
     */
    @RetryableTopic(
        attempts = "3",
        backoff = @Backoff(delay = 1000, multiplier = 2),
        dltStrategy = DltStrategy.FAIL_ON_ERROR,
        dltTopicSuffix = ".DLT"
    )
    @KafkaListener(topics = "user-events", groupId = "tasks-service-group")
    @Transactional
    public void handleUserEvent(
            @Payload String message,
            Acknowledgment acknowledgment,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        
        // Créer une clé unique pour l'événement (partition + offset)
        String eventKey = String.format("user-events-%d-%d", partition, offset);
        
        // Vérifier l'idempotence (éviter les doublons en cas de retry)
        if (processedEventRepository.existsByEventKey(eventKey)) {
            log.warn("Event already processed (idempotence check): partition={}, offset={}", partition, offset);
            acknowledgment.acknowledge();
            return;
        }
        
        try {
            log.info("Received user event from partition {} offset {}: {}", partition, offset, message);
            
            try {
                UserEvent userEvent = objectMapper.readValue(message, UserEvent.class);
                processUserEvent(userEvent);
                
                // Marquer l'événement comme traité (idempotence)
                ProcessedEvent processedEvent = ProcessedEvent.builder()
                        .eventKey(eventKey)
                        .build();
                processedEventRepository.save(processedEvent);
                
                acknowledgment.acknowledge();
                log.debug("User event processed and acknowledged: partition={}, offset={}", partition, offset);
            } catch (JsonProcessingException e) {
                log.warn("Failed to parse JSON, trying string parsing: {}", e.getMessage());
                processStringEvent(message);
                
                // Marquer comme traité même pour le fallback
                ProcessedEvent processedEvent = ProcessedEvent.builder()
                        .eventKey(eventKey)
                        .build();
                processedEventRepository.save(processedEvent);
                
                acknowledgment.acknowledge();
            }
        } catch (Exception e) {
            log.error("Error processing user event from partition {} offset {}: {}", 
                partition, offset, message, e);
            throw new RuntimeException("Failed to process user event", e);
        }
    }
    
    /**
     * Traite les messages en échec après tous les retries (Dead Letter Handler).
     * <p>
     * Appelé automatiquement par Spring Kafka Retry après épuisement des retries.
     * </p>
     *
     * @param message Le message en échec
     * @param acknowledgment Pour commit manuel
     */
    @DltHandler
    public void handleDeadLetter(
            @Payload String message,
            Acknowledgment acknowledgment,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        log.error("⚠️ DEAD LETTER - User event failed after all retries. " +
                "Partition: {}, Offset: {}, Message: {}", partition, offset, message);
        
        // TODO: En production, envoyer une alerte ou sauvegarder pour analyse
        
        acknowledgment.acknowledge();
    }
    
    private void processUserEvent(UserEvent userEvent) {
        Long userId = userEvent.getUserId();
        
        switch (userEvent.getEventType().toUpperCase()) {
            case "DELETED":
            case "user.deleted":
                handleUserDeleted(userId);
                break;
            case "UPDATED":
            case "user.updated":
                userCacheService.evictUser(userId);
                log.info("User {} updated, cache evicted", userId);
                break;
            case "CREATED":
            case "user.created":
                userCacheService.evictUser(userId);
                log.info("User {} created, cache evicted", userId);
                break;
            default:
                log.warn("Unknown user event type: {}", userEvent.getEventType());
        }
    }
    
    private void processStringEvent(String message) {
        if (message.contains("user.deleted")) {
            try {
                int startIdx = message.lastIndexOf('(') + 1;
                int endIdx = message.lastIndexOf(')');
                if (startIdx > 0 && endIdx > startIdx) {
                    Long userId = Long.parseLong(message.substring(startIdx, endIdx));
                    handleUserDeleted(userId);
                }
            } catch (Exception e) {
                log.error("Could not extract userId from message: {}", message, e);
            }
        } else if (message.contains("user.updated") || message.contains("user.created")) {
            userCacheService.evictAllUsers();
            log.info("User event received, all cache evicted");
        }
    }
    
    private void handleUserDeleted(Long userId) {
        log.warn("User {} deleted. Handling orphaned tasks.", userId);
        
        long deletedCount = taskRepository.findByUserId(userId).stream()
                .peek(task -> taskRepository.delete(task))
                .count();
        
        log.info("Deleted {} tasks for deleted user {}", deletedCount, userId);
        userCacheService.evictUser(userId);
    }
}

