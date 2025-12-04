package com.simdev.api_tasks.shared.repository;

import com.simdev.api_tasks.shared.domain.ProcessedEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, Long> {
    Optional<ProcessedEvent> findByEventKey(String eventKey);
    boolean existsByEventKey(String eventKey);
}

