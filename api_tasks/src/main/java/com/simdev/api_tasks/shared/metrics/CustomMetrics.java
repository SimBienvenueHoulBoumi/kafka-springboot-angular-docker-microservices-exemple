package com.simdev.api_tasks.shared.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Service pour les métriques personnalisées du service Tasks.
 * <p>
 * Ce service expose des métriques Prometheus pour monitorer
 * les opérations sur les tâches (création, mise à jour, suppression, récupération).
 * </p>
 *
 * @author API Tasks Service
 * @version 1.0
 */
@Component
@RequiredArgsConstructor
public class CustomMetrics {
    
    private final MeterRegistry meterRegistry;
    
    private Counter taskCreatedCounter;
    private Counter taskUpdatedCounter;
    private Counter taskDeletedCounter;
    private Counter taskRetrievalCounter;
    private Timer taskOperationTimer;
    
    @PostConstruct
    public void init() {
        taskCreatedCounter = Counter.builder("task.created.total")
                .description("Total number of tasks created")
                .register(meterRegistry);
        
        taskUpdatedCounter = Counter.builder("task.updated.total")
                .description("Total number of tasks updated")
                .register(meterRegistry);
        
        taskDeletedCounter = Counter.builder("task.deleted.total")
                .description("Total number of tasks deleted")
                .register(meterRegistry);
        
        taskRetrievalCounter = Counter.builder("task.retrieval.total")
                .description("Total number of task retrievals")
                .register(meterRegistry);
        
        taskOperationTimer = Timer.builder("task.operation.duration")
                .description("Time taken for task operations")
                .register(meterRegistry);
    }
    
    public void incrementTaskCreated() {
        taskCreatedCounter.increment();
    }
    
    public void incrementTaskUpdated() {
        taskUpdatedCounter.increment();
    }
    
    public void incrementTaskDeleted() {
        taskDeletedCounter.increment();
    }
    
    public void incrementTaskRetrieval() {
        taskRetrievalCounter.increment();
    }
    
    public Timer.Sample startTaskOperationTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordTaskOperationDuration(Timer.Sample sample) {
        sample.stop(taskOperationTimer);
    }
}

