package com.simdev.api_tasks.shared.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entité pour tracker les événements déjà traités (idempotence).
 * <p>
 * Permet d'éviter le traitement en double d'un même événement en cas de retry Kafka.
 * </p>
 */
@Entity
@Table(name = "processed_events", 
       indexes = @Index(name = "idx_event_key", columnList = "eventKey", unique = true))
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true, length = 255)
    private String eventKey;
    
    @Column(nullable = false)
    private LocalDateTime processedAt;
    
    @PrePersist
    protected void onCreate() {
        processedAt = LocalDateTime.now();
    }
}

