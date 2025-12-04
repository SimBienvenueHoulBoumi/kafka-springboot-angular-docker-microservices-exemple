package com.simdev.api_users.shared.service;

import com.simdev.api_users.shared.domain.OutboxEvent;
import com.simdev.api_users.shared.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Service pour gérer le pattern Transactional Outbox.
 * <p>
 * Ce service :
 * - Crée des événements dans la table outbox lors des opérations métier
 * - Publie périodiquement les événements en attente vers Kafka
 * - Gère les retries en cas d'échec de publication
 * </p>
 *
 * @author API Users Service
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {
    
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    private static final int MAX_RETRIES = 3;
    private static final int BATCH_SIZE = 10;
    
    /**
     * Crée un événement dans l'outbox pour publication ultérieure.
     * 
     * @param eventType Le type d'événement (ex: "user.created")
     * @param topic Le topic Kafka (ex: "user-events")
     * @param payload Le contenu JSON de l'événement
     * @param partitionKey La clé de partition (optionnelle)
     * @return L'événement créé
     */
    @Transactional
    public OutboxEvent createEvent(String eventType, String topic, String payload, String partitionKey) {
        OutboxEvent event = OutboxEvent.builder()
                .eventType(eventType)
                .topic(topic)
                .payload(payload)
                .partitionKey(partitionKey)
                .status(OutboxEvent.EventStatus.PENDING)
                .retryCount(0)
                .build();
        
        return outboxEventRepository.save(event);
    }
    
    /**
     * Publie périodiquement les événements en attente vers Kafka.
     * <p>
     * Exécuté toutes les 5 secondes. Traite jusqu'à 10 événements par exécution.
     * </p>
     */
    @Scheduled(fixedRate = 5000) // Toutes les 5 secondes
    @Transactional
    public void publishPendingEvents() {
        try {
            List<OutboxEvent> pendingEvents = outboxEventRepository.findPendingEvents();
            
            if (pendingEvents.isEmpty()) {
                return;
            }
            
            int processed = 0;
            for (OutboxEvent event : pendingEvents) {
                if (processed >= BATCH_SIZE) {
                    break;
                }
                
                try {
                    // Marquer comme en cours de traitement
                    outboxEventRepository.markAsProcessing(event.getId(), LocalDateTime.now());
                    
                    // Publier sur Kafka
                    String key = event.getPartitionKey() != null ? event.getPartitionKey() : event.getId().toString();
                    
                    kafkaTemplate.send(event.getTopic(), key, event.getPayload())
                        .whenComplete((result, ex) -> {
                            if (ex == null) {
                                outboxEventRepository.markAsPublished(event.getId(), LocalDateTime.now());
                                log.debug("Outbox event {} published successfully to topic {}", 
                                    event.getId(), event.getTopic());
                            } else {
                                handlePublishFailure(event, ex);
                            }
                        });
                    
                    processed++;
                } catch (Exception e) {
                    handlePublishFailure(event, e);
                }
            }
            
            if (processed > 0) {
                log.debug("Processed {} outbox events", processed);
            }
        } catch (Exception e) {
            log.error("Error processing outbox events", e);
        }
    }
    
    /**
     * Gère les échecs de publication avec retry.
     */
    private void handlePublishFailure(OutboxEvent event, Throwable error) {
        int newRetryCount = event.getRetryCount() + 1;
        String errorMessage = error.getMessage();
        
        if (newRetryCount >= MAX_RETRIES) {
            outboxEventRepository.incrementRetry(event.getId(), errorMessage, OutboxEvent.EventStatus.FAILED);
            log.error("Outbox event {} failed after {} retries. Event: {}", 
                event.getId(), MAX_RETRIES, event.getEventType(), error);
        } else {
            outboxEventRepository.incrementRetry(event.getId(), errorMessage, OutboxEvent.EventStatus.PENDING);
            log.warn("Outbox event {} publication failed (retry {}/{}). Will retry later. Error: {}", 
                event.getId(), newRetryCount, MAX_RETRIES, errorMessage);
        }
    }
    
    /**
     * Nettoie les anciens événements publiés (plus de 24h).
     */
    @Scheduled(cron = "0 0 2 * * ?") // Tous les jours à 2h du matin
    @Transactional
    public void cleanupOldEvents() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(1);
        outboxEventRepository.deleteOldPublishedEvents(cutoffDate);
        log.debug("Cleanup old published outbox events scheduled");
    }
}

