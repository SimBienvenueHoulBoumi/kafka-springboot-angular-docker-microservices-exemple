package com.simdev.api_tasks.task.service;

import com.simdev.api_tasks.task.domain.Task;
import com.simdev.api_tasks.task.dto.TaskEvent;
import com.simdev.api_tasks.task.dto.TaskRequest;
import com.simdev.api_tasks.task.dto.TaskResponse;
import com.simdev.api_tasks.task.exception.ResourceNotFoundException;
import com.simdev.api_tasks.task.exception.UnauthorizedAccessException;
import com.simdev.api_tasks.task.repository.TaskRepository;
import com.simdev.api_tasks.shared.metrics.CustomMetrics;
import com.simdev.api_tasks.shared.service.EventService;
import com.simdev.api_tasks.shared.service.UserCacheService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Service de gestion des tâches (opérations CRUD).
 * <p>
 * Ce service gère toutes les opérations sur les tâches :
 * <ul>
 *   <li><strong>Création :</strong> Validation de l'existence de l'utilisateur et publication d'événement</li>
 *   <li><strong>Lecture :</strong> Récupération de tâches avec contrôle d'accès (propriétaire ou admin)</li>
 *   <li><strong>Mise à jour :</strong> Modification des tâches avec vérification des permissions</li>
 *   <li><strong>Suppression :</strong> Suppression sécurisée avec publication d'événement</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Validation des utilisateurs :</strong>
 * Avant toute opération nécessitant un utilisateur, le service valide l'existence
 * de l'utilisateur via {@link UserCacheService}. Ce service utilise un cache en mémoire
 * avec circuit breaker pour éviter les appels répétés au service Users.
 * </p>
 * <p>
 * <strong>Contrôle d'accès (RBAC) :</strong>
 * <ul>
 *   <li>Les utilisateurs normaux (ROLE_USER) ne peuvent gérer que leurs propres tâches</li>
 *   <li>Les administrateurs (ROLE_ADMIN) peuvent gérer toutes les tâches</li>
 *   <li>Les tentatives d'accès non autorisé lèvent {@link UnauthorizedAccessException}</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Événements Kafka :</strong>
 * Toutes les modifications de tâches déclenchent la publication d'événements sur Kafka :
 * <ul>
 *   <li>{@code task.created} - Lors de la création d'une tâche</li>
 *   <li>{@code task.updated} - Lors de la mise à jour d'une tâche</li>
 *   <li>{@code task.deleted} - Lors de la suppression d'une tâche</li>
 * </ul>
 * Les événements sont publiés via le pattern Transactional Outbox pour garantir la livraison.
 * </p>
 * <p>
 * <strong>Métriques :</strong>
 * Le service enregistre automatiquement des métriques via {@link CustomMetrics} :
 * nombre de tâches créées, mises à jour et supprimées.
 * </p>
 *
 * @author API Tasks Service
 * @version 1.0
 * @see EventService
 * @see UserCacheService
 * @see CustomMetrics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {
    
    private final TaskRepository taskRepository;
    private final EventService eventService;
    private final UserCacheService userCacheService;
    private final CustomMetrics customMetrics;
    
    /**
     * Crée une nouvelle tâche pour un utilisateur.
     * <p>
     * Valide que l'utilisateur existe avant de créer la tâche.
     * Un événement Kafka est publié après la création.
     * </p>
     *
     * @param request Les informations de la tâche à créer
     * @param userId L'identifiant unique de l'utilisateur propriétaire de la tâche
     * @return La tâche créée
     */
    @Transactional
    public TaskResponse createTask(TaskRequest request, Long userId) {
        userCacheService.userExists(userId);
        
        Task task = Task.builder()
                .title(request.getTitle())
                .description(request.getDescription())
                .status(request.getStatus() != null ? request.getStatus() : Task.TaskStatus.PENDING)
                .userId(userId)
                .build();
        
        task = taskRepository.save(task);
        
        TaskEvent taskEvent = TaskEvent.builder()
                .eventType(TaskEvent.EventType.CREATED)
                .taskId(task.getId())
                .userId(task.getUserId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus().name())
                .build();
        eventService.publishTaskEvent(taskEvent);
        
        log.info("Task created: {} for user {}", task.getTitle(), task.getUserId());
        customMetrics.incrementTaskCreated();
        return mapToResponse(task);
    }
    
    /**
     * Récupère toutes les tâches d'un utilisateur.
     * Si isAdmin est true, retourne toutes les tâches de tous les utilisateurs.
     *
     * @param userId L'identifiant unique de l'utilisateur
     * @param isAdmin true si l'utilisateur est admin, false sinon
     * @return La liste des tâches
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByUserId(Long userId, boolean isAdmin) {
        if (isAdmin) {
            return taskRepository.findAll().stream()
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }
        return taskRepository.findByUserId(userId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    /**
     * Récupère toutes les tâches d'un utilisateur (pour rétrocompatibilité).
     *
     * @param userId L'identifiant unique de l'utilisateur
     * @return La liste des tâches de l'utilisateur
     */
    @Transactional(readOnly = true)
    public List<TaskResponse> getTasksByUserId(Long userId) {
        return getTasksByUserId(userId, false);
    }
    
    /**
     * Récupère une tâche par son identifiant unique.
     * <p>
     * Vérifie que la tâche appartient bien à l'utilisateur spécifié, sauf si l'utilisateur est admin.
     * </p>
     *
     * @param id L'identifiant unique de la tâche
     * @param userId L'identifiant unique de l'utilisateur propriétaire
     * @param isAdmin true si l'utilisateur est admin, false sinon
     * @return La tâche demandée
     * @throws ResourceNotFoundException si la tâche n'existe pas
     * @throws UnauthorizedAccessException si la tâche n'appartient pas à l'utilisateur et que l'utilisateur n'est pas admin
     */
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id, Long userId, boolean isAdmin) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
        
        if (!isAdmin && !task.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException(
                "Vous n'avez pas l'autorisation de consulter cette tâche");
        }
        
        return mapToResponse(task);
    }
    
    /**
     * Récupère une tâche par son identifiant unique (pour rétrocompatibilité).
     *
     * @param id L'identifiant unique de la tâche
     * @param userId L'identifiant unique de l'utilisateur propriétaire
     * @return La tâche demandée
     * @throws ResourceNotFoundException si la tâche n'existe pas
     * @throws UnauthorizedAccessException si la tâche n'appartient pas à l'utilisateur
     */
    @Transactional(readOnly = true)
    public TaskResponse getTaskById(Long id, Long userId) {
        return getTaskById(id, userId, false);
    }
    
    /**
     * Met à jour une tâche existante.
     * <p>
     * Vérifie que la tâche appartient bien à l'utilisateur spécifié avant de la modifier, sauf si l'utilisateur est admin.
     * Un événement Kafka est publié après la mise à jour.
     * </p>
     *
     * @param id L'identifiant unique de la tâche à mettre à jour
     * @param request Les nouvelles informations de la tâche
     * @param userId L'identifiant unique de l'utilisateur propriétaire
     * @param isAdmin true si l'utilisateur est admin, false sinon
     * @return La tâche mise à jour
     * @throws ResourceNotFoundException si la tâche n'existe pas
     * @throws UnauthorizedAccessException si la tâche n'appartient pas à l'utilisateur et que l'utilisateur n'est pas admin
     */
    @Transactional
    public TaskResponse updateTask(Long id, TaskRequest request, Long userId, boolean isAdmin) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
        
        if (!isAdmin && !task.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException(
                "Vous n'avez pas l'autorisation de modifier cette tâche");
        }
        
        task.setTitle(request.getTitle());
        task.setDescription(request.getDescription());
        if (request.getStatus() != null) {
            task.setStatus(request.getStatus());
        }
        
        task = taskRepository.save(task);
        
        TaskEvent taskEvent = TaskEvent.builder()
                .eventType(TaskEvent.EventType.UPDATED)
                .taskId(task.getId())
                .userId(task.getUserId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus().name())
                .build();
        eventService.publishTaskEvent(taskEvent);
        
        log.info("Task updated: {}", task.getTitle());
        customMetrics.incrementTaskUpdated();
        return mapToResponse(task);
    }
    
    /**
     * Met à jour une tâche existante (pour rétrocompatibilité).
     *
     * @param id L'identifiant unique de la tâche à mettre à jour
     * @param request Les nouvelles informations de la tâche
     * @param userId L'identifiant unique de l'utilisateur propriétaire
     * @return La tâche mise à jour
     * @throws ResourceNotFoundException si la tâche n'existe pas
     * @throws UnauthorizedAccessException si la tâche n'appartient pas à l'utilisateur
     */
    @Transactional
    public TaskResponse updateTask(Long id, TaskRequest request, Long userId) {
        return updateTask(id, request, userId, false);
    }
    
    /**
     * Supprime une tâche du système.
     * <p>
     * Vérifie que la tâche appartient bien à l'utilisateur spécifié avant de la supprimer, sauf si l'utilisateur est admin.
     * Cette opération est irréversible. Un événement Kafka est publié après la suppression.
     * </p>
     *
     * @param id L'identifiant unique de la tâche à supprimer
     * @param userId L'identifiant unique de l'utilisateur propriétaire
     * @param isAdmin true si l'utilisateur est admin, false sinon
     * @throws ResourceNotFoundException si la tâche n'existe pas
     * @throws UnauthorizedAccessException si la tâche n'appartient pas à l'utilisateur et que l'utilisateur n'est pas admin
     */
    @Transactional
    public void deleteTask(Long id, Long userId, boolean isAdmin) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Task", id));
        
        if (!isAdmin && !task.getUserId().equals(userId)) {
            throw new UnauthorizedAccessException(
                "Vous n'avez pas l'autorisation de supprimer cette tâche");
        }
        
        Long taskId = task.getId();
        Long taskUserId = task.getUserId();
        String title = task.getTitle();
        
        taskRepository.delete(task);
        
        TaskEvent deleteEvent = TaskEvent.builder()
                .eventType(TaskEvent.EventType.DELETED)
                .taskId(taskId)
                .userId(taskUserId)
                .title(title)
                .status("DELETED")
                .build();
        eventService.publishTaskEvent(deleteEvent);
        
        log.info("Task deleted: {}", title);
        customMetrics.incrementTaskDeleted();
    }
    
    /**
     * Supprime une tâche du système (pour rétrocompatibilité).
     *
     * @param id L'identifiant unique de la tâche à supprimer
     * @param userId L'identifiant unique de l'utilisateur propriétaire
     * @throws ResourceNotFoundException si la tâche n'existe pas
     * @throws UnauthorizedAccessException si la tâche n'appartient pas à l'utilisateur
     */
    @Transactional
    public void deleteTask(Long id, Long userId) {
        deleteTask(id, userId, false);
    }
    
    private TaskResponse mapToResponse(Task task) {
        return TaskResponse.builder()
                .id(task.getId())
                .title(task.getTitle())
                .description(task.getDescription())
                .status(task.getStatus())
                .userId(task.getUserId())
                .createdAt(task.getCreatedAt())
                .updatedAt(task.getUpdatedAt())
                .build();
    }
}

