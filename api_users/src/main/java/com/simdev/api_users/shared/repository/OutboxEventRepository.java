package com.simdev.api_users.shared.repository;

import com.simdev.api_users.shared.domain.OutboxEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository pour gérer les événements Outbox.
 */
@Repository
public interface OutboxEventRepository extends JpaRepository<OutboxEvent, Long> {
    
    /**
     * Récupère les événements en attente de publication, avec verrouillage pessimiste
     * pour éviter le traitement simultané par plusieurs instances.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT e FROM OutboxEvent e WHERE e.status = 'PENDING' ORDER BY e.createdAt ASC")
    List<OutboxEvent> findPendingEvents();
    
    /**
     * Marque un événement comme étant en cours de traitement.
     */
    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = 'PROCESSING', e.processedAt = :now WHERE e.id = :id")
    void markAsProcessing(@Param("id") Long id, @Param("now") LocalDateTime now);
    
    /**
     * Marque un événement comme publié avec succès.
     */
    @Modifying
    @Query("UPDATE OutboxEvent e SET e.status = 'PUBLISHED', e.processedAt = :now WHERE e.id = :id")
    void markAsPublished(@Param("id") Long id, @Param("now") LocalDateTime now);
    
    /**
     * Incrémente le compteur de retry et met à jour le message d'erreur.
     */
    @Modifying
    @Query("UPDATE OutboxEvent e SET e.retryCount = e.retryCount + 1, e.errorMessage = :errorMessage, e.status = :status WHERE e.id = :id")
    void incrementRetry(@Param("id") Long id, @Param("errorMessage") String errorMessage, @Param("status") OutboxEvent.EventStatus status);
    
    /**
     * Supprime les événements publiés après une certaine période (nettoyage).
     */
    @Modifying
    @Query("DELETE FROM OutboxEvent e WHERE e.status = 'PUBLISHED' AND e.processedAt < :cutoffDate")
    void deleteOldPublishedEvents(@Param("cutoffDate") LocalDateTime cutoffDate);
}

