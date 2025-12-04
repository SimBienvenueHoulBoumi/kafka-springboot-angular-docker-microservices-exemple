package com.simdev.api_tasks.shared.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "outbox_events")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OutboxEvent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, length = 100)
    private String eventType;
    
    @Column(nullable = false, length = 100)
    private String topic;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String payload;
    
    @Column(length = 255)
    private String partitionKey;
    
    @Column(nullable = false)
    @Builder.Default
    @Enumerated(EnumType.STRING)
    private EventStatus status = EventStatus.PENDING;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column
    private LocalDateTime processedAt;
    
    @Column
    private Integer retryCount;
    
    @Column(columnDefinition = "TEXT")
    private String errorMessage;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (retryCount == null) {
            retryCount = 0;
        }
    }
    
    public enum EventStatus {
        PENDING, PROCESSING, PUBLISHED, FAILED
    }
}

