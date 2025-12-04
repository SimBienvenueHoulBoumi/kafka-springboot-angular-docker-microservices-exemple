package com.simdev.api_tasks.task.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class TaskEvent {
    private EventType eventType;
    private Long taskId;
    private Long userId;
    private String title;
    private String description;
    private String status;
    private LocalDateTime timestamp;
    
    @JsonCreator
    public TaskEvent(
            @JsonProperty("eventType") EventType eventType,
            @JsonProperty("taskId") Long taskId,
            @JsonProperty("userId") Long userId,
            @JsonProperty("title") String title,
            @JsonProperty("description") String description,
            @JsonProperty("status") String status,
            @JsonProperty("timestamp") LocalDateTime timestamp) {
        this.eventType = eventType;
        this.taskId = taskId;
        this.userId = userId;
        this.title = title;
        this.description = description;
        this.status = status;
        this.timestamp = timestamp != null ? timestamp : LocalDateTime.now();
    }
    
    public enum EventType {
        CREATED, UPDATED, DELETED
    }
}

