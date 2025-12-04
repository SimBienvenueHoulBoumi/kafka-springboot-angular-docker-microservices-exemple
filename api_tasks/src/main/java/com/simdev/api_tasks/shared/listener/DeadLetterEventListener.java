package com.simdev.api_tasks.shared.listener;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Listener pour les événements en échec (Dead Letter Queue).
 * <p>
 * Ce listener traite les messages qui ont échoué après tous les retries.
 * Permet de logger et monitorer les événements non traitables.
 * </p>
 *
 * @author API Tasks Service
 * @version 1.0
 */
@Component
@Slf4j
public class DeadLetterEventListener {
    
    /**
     * Traite les messages en échec depuis le Dead Letter Topic.
     * <p>
     * Ces messages ont échoué après tous les retries configurés.
     * Il faut investiguer manuellement pourquoi ces messages ne peuvent pas être traités.
     * </p>
     *
     * @param message Le message en échec
     * @param acknowledgment Pour commit manuel
     * @param partition La partition Kafka
     * @param offset L'offset du message
     */
    @KafkaListener(topics = "user-events.DLT", groupId = "tasks-service-dlt-group")
    public void handleDeadLetterMessage(
            @Payload String message,
            Acknowledgment acknowledgment,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset) {
        
        log.error("⚠️ DEAD LETTER MESSAGE - Failed to process user event after all retries. " +
                "Partition: {}, Offset: {}, Message: {}", partition, offset, message);
        
        // TODO: En production, envoyer une alerte (email, Slack, etc.)
        // ou sauvegarder dans une table de monitoring pour analyse
        
        acknowledgment.acknowledge();
    }
}

