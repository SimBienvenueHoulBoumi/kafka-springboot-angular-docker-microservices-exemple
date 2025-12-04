package com.simdev.api_users.shared.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entité représentant un événement en attente de publication sur Kafka.
 * <p>
 * Implémente le pattern Transactional Outbox pour garantir la livraison des événements :
 * - L'événement est sauvegardé en base de données dans la même transaction que l'entité métier
 * - Un processus asynchrone publie ensuite les événements sur Kafka
 * - Évite la perte d'événements si Kafka est indisponible lors de la transaction
 * </p>
 *
 * @author API Users Service
 * @version 1.0
 */
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
        PENDING,    // En attente de publication
        PROCESSING, // En cours de publication
        PUBLISHED,  // Publié avec succès
        FAILED      // Échec après tous les retries
    }
}

