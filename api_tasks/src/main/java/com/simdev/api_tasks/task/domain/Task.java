package com.simdev.api_tasks.task.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entité JPA représentant une tâche du système.
 * <p>
 * Cette entité représente une tâche assignée à un utilisateur avec son titre, description,
 * statut et informations d'audit (dates de création et de modification).
 * </p>
 * <p>
 * <strong>Informations stockées :</strong>
 * <ul>
 *   <li><strong>Identifiants :</strong> ID unique de la tâche, ID de l'utilisateur propriétaire</li>
 *   <li><strong>Contenu :</strong> Titre (obligatoire), description (optionnelle, max 1000 caractères)</li>
 *   <li><strong>Statut :</strong> État de la tâche ({@link TaskStatus})</li>
 *   <li><strong>Audit :</strong> Date de création et de dernière mise à jour</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Statuts disponibles :</strong>
 * <ul>
 *   <li>{@code PENDING} - Tâche en attente (statut par défaut)</li>
 *   <li>{@code IN_PROGRESS} - Tâche en cours d'exécution</li>
 *   <li>{@code COMPLETED} - Tâche terminée</li>
 *   <li>{@code CANCELLED} - Tâche annulée</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Relations :</strong>
 * <ul>
 *   <li>Chaque tâche appartient à un utilisateur identifié par {@code userId}</li>
 *   <li>Lorsqu'un utilisateur est supprimé, ses tâches sont automatiquement supprimées
 *       via le listener Kafka {@link com.simdev.api_tasks.shared.listener.UserEventListener}</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Callbacks JPA :</strong>
 * <ul>
 *   <li>{@code @PrePersist} : Initialise {@code createdAt} et {@code updatedAt} à la création</li>
 *   <li>{@code @PreUpdate} : Met à jour {@code updatedAt} à chaque modification</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Événements Kafka :</strong>
 * Toute modification (création, mise à jour, suppression) déclenche la publication
 * d'un événement sur Kafka via le pattern Transactional Outbox.
 * </p>
 *
 * @author API Tasks Service
 * @version 1.0
 * @see TaskStatus
 * @see com.simdev.api_tasks.task.repository.TaskRepository
 * @see com.simdev.api_tasks.task.service.TaskService
 */
@Entity
@Table(name = "tasks")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Task {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String title;
    
    @Column(length = 1000)
    private String description;
    
    @Column(nullable = false)
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TaskStatus status = TaskStatus.PENDING;
    
    @Column(nullable = false)
    private Long userId;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    public enum TaskStatus {
        PENDING, IN_PROGRESS, COMPLETED, CANCELLED
    }
}

