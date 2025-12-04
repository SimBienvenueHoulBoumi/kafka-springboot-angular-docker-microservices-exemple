package com.simdev.api_tasks.shared.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simdev.api_tasks.task.dto.TaskEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service de publication d'événements Kafka pour les tâches.
 * <p>
 * Ce service utilise le pattern <strong>Transactional Outbox</strong> pour garantir
 * la livraison garantie des événements dans une architecture distribuée.
 * </p>
 * <p>
 * <strong>Pattern Transactional Outbox :</strong>
 * <ol>
 *   <li>L'événement est sérialisé en JSON</li>
 *   <li>Il est sauvegardé dans la table {@code outbox_events} dans la MÊME transaction
 *       que l'entité métier (tâche), garantissant l'atomicité</li>
 *   <li>Le service {@link OutboxService} publie ensuite les événements sur Kafka
 *       de manière asynchrone via un processus périodique</li>
 *   <li>En cas d'indisponibilité de Kafka, les événements restent en attente et seront
 *       publiés dès que Kafka sera disponible</li>
 * </ol>
 * </p>
 * <p>
 * <strong>Avantages :</strong>
 * <ul>
 *   <li>Garantit la cohérence transactionnelle (pas de perte d'événements)</li>
 *   <li>Résilience face aux pannes de Kafka</li>
 *   <li>Retry automatique avec gestion des échecs</li>
 *   <li>Idempotence via les clés de partition</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Types d'événements publiés :</strong>
 * <ul>
 *   <li>{@code task.created} - Tâche créée</li>
 *   <li>{@code task.updated} - Tâche mise à jour</li>
 *   <li>{@code task.deleted} - Tâche supprimée</li>
 * </ul>
 * Tous les événements sont publiés sur le topic Kafka {@code task-events}.
 * </p>
 *
 * @author API Tasks Service
 * @version 2.0
 * @see OutboxService
 * @see TaskEvent
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class EventService {
    
    private final OutboxService outboxService;
    private final ObjectMapper objectMapper;
    private static final String TASK_TOPIC = "task-events";
    
    /**
     * Crée un événement de tâche dans l'outbox pour publication ultérieure.
     * <p>
     * L'événement est sauvegardé en base de données dans la même transaction que la tâche,
     * garantissant qu'il sera publié même si Kafka est temporairement indisponible.
     * </p>
     *
     * @param taskEvent L'événement de tâche à publier
     */
    @Transactional
    public void publishTaskEvent(TaskEvent taskEvent) {
        try {
            String eventJson = objectMapper.writeValueAsString(taskEvent);
            String key = taskEvent.getTaskId() != null ? taskEvent.getTaskId().toString() : "unknown";
            String eventType = "task." + taskEvent.getEventType().name().toLowerCase();
            
            outboxService.createEvent(eventType, TASK_TOPIC, eventJson, key);
            
            log.debug("Task event queued in outbox: {} - {}", 
                taskEvent.getEventType(), taskEvent.getTaskId());
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize task event: {}", taskEvent, e);
        } catch (Exception e) {
            log.error("Failed to create outbox event for task: {} - {}", 
                taskEvent.getEventType(), taskEvent.getTaskId(), e);
        }
    }
}

