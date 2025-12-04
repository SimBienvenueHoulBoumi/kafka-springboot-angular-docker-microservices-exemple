package com.simdev.api_tasks.shared.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO représentant un événement utilisateur depuis Kafka.
 * <p>
 * Ce DTO est utilisé pour désérialiser les événements utilisateurs
 * reçus depuis le topic "user-events" de Kafka.
 * </p>
 *
 * @author API Tasks Service
 * @version 1.0
 */
@Data
public class UserEvent {
    private String eventType;
    private Long userId;
    private String email;
    private LocalDateTime timestamp;
    
    @JsonCreator
    public UserEvent(
            @JsonProperty("eventType") String eventType,
            @JsonProperty("userId") Long userId,
            @JsonProperty("email") String email,
            @JsonProperty("timestamp") LocalDateTime timestamp) {
        this.eventType = eventType;
        this.userId = userId;
        this.email = email;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }
}

