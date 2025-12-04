package com.simdev.api_tasks.shared.repository;

import com.simdev.api_tasks.shared.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 'PENDING' ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPendingEvents();
    
    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = 'PROCESSING', e.processedAt = :now WHERE e.id = :id")
    void markAsProcessing(@Param("id") Long id, @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = 'PUBLISHED', e.processedAt = :now WHERE e.id = :id")
    void markAsPublished(@Param("id") Long id, @Param("now") LocalDateTime now);
    
    @Modifying
    @Query("UPDATE OutboxEvent e SET e.retryCount = e.retryCount + 1, e.errorMessage = :errorMessage, e.status = :status WHERE e.id = :id")
    void incrementRetry(@Param("id") Long id, @Param("errorMessage") String errorMessage, @Param("status") OutboxEvent.EventStatus status);
    
    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.status = 'PUBLISHED' AND e.processedAt < :cutoffDate")
    void deleteOldPublishedEvents(@Param("cutoffDate") LocalDateTime cutoffDate);
}

