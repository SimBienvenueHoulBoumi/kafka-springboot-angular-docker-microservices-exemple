package com.simdev.api_tasks.shared.service;

import com.simdev.api_tasks.shared.domain.OutboxEvent;
import com.simdev.api_tasks.shared.repository.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class OutboxService {
    
    private final OutboxEventRepository outboxEventRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    
    private static final int MAX_RETRIES = 3;
    private static final int BATCH_SIZE = 10;
    
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
    
    @Scheduled(fixedRate = 5000)
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
                    outboxEventRepository.markAsProcessing(event.getId(), LocalDateTime.now());
                    
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
    
    @Scheduled(cron = "0 0 2 * * ?")
    @Transactional
    public void cleanupOldEvents() {
        LocalDateTime cutoffDate = LocalDateTime.now().minusDays(1);
        outboxEventRepository.deleteOldPublishedEvents(cutoffDate);
        log.debug("Cleanup old published outbox events scheduled");
    }
}

