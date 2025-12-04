package com.simdev.api_tasks.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simdev.api_tasks.task.dto.TaskEvent;
import com.simdev.api_tasks.shared.service.EventService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@SuppressWarnings("null")
@ExtendWith(MockitoExtension.class)
@DisplayName("EventService Tests")
class EventServiceTest {

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @InjectMocks
    private EventService eventService;
    
    private ObjectMapper objectMapper;

    private TaskEvent taskEvent;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        // Inject ObjectMapper using reflection
        ReflectionTestUtils.setField(eventService, "objectMapper", objectMapper);
        
        taskEvent = TaskEvent.builder()
                .eventType(TaskEvent.EventType.CREATED)
                .taskId(1L)
                .userId(1L)
                .title("Test Task")
                .description("Test Description")
                .status("PENDING")
                .build();
    }

    @Test
    @DisplayName("Should publish task event to Kafka")
    void testPublishTaskEvent_Success() throws JsonProcessingException {
        // When
        eventService.publishTaskEvent(taskEvent);

        // Then
        ArgumentCaptor<String> valueCaptor = ArgumentCaptor.forClass(String.class);
        verify(kafkaTemplate).send(eq("task-events"), eq("1"), valueCaptor.capture());
        
        assertThat(valueCaptor.getValue()).contains("CREATED");
        assertThat(valueCaptor.getValue()).contains("Test Task");
    }
}

