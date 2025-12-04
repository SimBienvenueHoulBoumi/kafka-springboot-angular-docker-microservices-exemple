package com.simdev.api_tasks.task.controller;

import com.simdev.api_tasks.task.dto.TaskRequest;
import com.simdev.api_tasks.task.dto.TaskResponse;
import com.simdev.api_tasks.task.exception.ResourceNotFoundException;
import com.simdev.api_tasks.task.exception.UnauthorizedAccessException;
import com.simdev.api_tasks.task.service.TaskService;
import com.simdev.api_tasks.shared.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Contrôleur REST pour la gestion des tâches.
 * <p>
 * Ce contrôleur expose les endpoints pour la gestion complète des tâches (CRUD).
 * Tous les endpoints nécessitent une authentification JWT. L'ID de l'utilisateur
 * est automatiquement extrait du token JWT.
 * </p>
 * <p>
 * <strong>Permissions :</strong>
 * <ul>
 *   <li>Les utilisateurs normaux (ROLE_USER) ne peuvent accéder qu'à leurs propres tâches.</li>
 *   <li>Les administrateurs (ROLE_ADMIN) peuvent accéder à toutes les tâches (lecture, modification, suppression).</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Note :</strong> Ce contrôleur est appelé uniquement par l'API Gateway.
 * Il n'est pas accessible publiquement depuis l'extérieur.
 * </p>
 *
 * @author API Tasks Service
 * @version 1.0
 */
@RestController
@RequestMapping("/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "📝 API de gestion des tâches - Microservice Tasks. " +
        "Gestion complète des tâches (CRUD). " +
        "Les utilisateurs (ROLE_USER) accèdent uniquement à leurs propres tâches. " +
        "🔒 Les administrateurs (ROLE_ADMIN) ont accès à toutes les tâches. " +
        "Les événements tâches sont publiés sur Kafka (task.created, task.updated, task.deleted). " +
        "Le service écoute également les événements utilisateurs depuis Kafka pour synchronisation. " +
        "⚠️ Accessible uniquement via l'API Gateway sur /api/tasks/**")
public class TaskController {
    
    private final TaskService taskService;
    private final SecurityUtils securityUtils;
    
    /**
     * Crée une nouvelle tâche pour l'utilisateur authentifié.
     * <p>
     * L'ID de l'utilisateur est automatiquement extrait du token JWT.
     * La tâche est associée à l'utilisateur authentifié.
     * </p>
     *
     * @param request Les informations de la tâche à créer (titre, description, statut)
     * @param httpRequest La requête HTTP pour extraire l'ID de l'utilisateur depuis le token JWT
     * @return ResponseEntity contenant la tâche créée avec le statut HTTP 201 (Created)
     * @throws IllegalArgumentException si le token JWT est manquant ou invalide
     */
    @PostMapping
    @Operation(
        summary = "Créer une nouvelle tâche",
        description = "✅ Crée une tâche pour l'utilisateur authentifié. " +
                "L'userId est automatiquement extrait du token JWT (ne pas le fournir manuellement). " +
                "Valide l'existence de l'utilisateur via le service Users (avec cache). " +
                "Publie l'événement 'task.created' sur Kafka. " +
                "Route: POST /api/tasks (via API Gateway)"
    )
    public ResponseEntity<TaskResponse> createTask(
            @Valid @RequestBody TaskRequest request,
            HttpServletRequest httpRequest) {
        Long userId = securityUtils.getUserIdFromRequest(httpRequest);
        TaskResponse task = taskService.createTask(request, userId);
        return ResponseEntity.status(HttpStatus.CREATED).body(task);
    }
    
    /**
     * Récupère toutes les tâches de l'utilisateur authentifié.
     * <p>
     * Si l'utilisateur est admin (ROLE_ADMIN), retourne toutes les tâches de tous les utilisateurs.
     * Sinon, retourne uniquement les tâches appartenant à l'utilisateur authentifié.
     * L'ID de l'utilisateur est automatiquement extrait du token JWT.
     * </p>
     *
     * @param httpRequest La requête HTTP pour extraire l'ID de l'utilisateur depuis le token JWT
     * @return ResponseEntity contenant la liste des tâches avec le statut HTTP 200 (OK)
     * @throws IllegalArgumentException si le token JWT est manquant ou invalide
     */
    @GetMapping
    @Operation(
        summary = "Lister mes tâches / Toutes les tâches (Admin)",
        description = "✅ Retourne toutes les tâches de l'utilisateur authentifié. " +
                "🔒 Si l'utilisateur est ADMIN (ROLE_ADMIN), retourne toutes les tâches de tous les utilisateurs. " +
                "L'userId est extrait automatiquement du token JWT. " +
                "Route: GET /api/tasks (via API Gateway)"
    )
    public ResponseEntity<List<TaskResponse>> getAllTasks(HttpServletRequest httpRequest) {
        Long userId = securityUtils.getUserIdFromRequest(httpRequest);
        boolean isAdmin = securityUtils.isAdmin(httpRequest);
        List<TaskResponse> tasks = taskService.getTasksByUserId(userId, isAdmin);
        return ResponseEntity.ok(tasks);
    }
    
    /**
     * Récupère une tâche par son ID.
     * <p>
     * Si l'utilisateur est admin (ROLE_ADMIN), peut consulter n'importe quelle tâche.
     * Sinon, vérifie que la tâche appartient bien à l'utilisateur authentifié.
     * L'ID de l'utilisateur est automatiquement extrait du token JWT.
     * </p>
     *
     * @param id L'identifiant unique de la tâche à récupérer
     * @param httpRequest La requête HTTP pour extraire l'ID de l'utilisateur depuis le token JWT
     * @return ResponseEntity contenant la tâche demandée avec le statut HTTP 200 (OK)
     * @throws ResourceNotFoundException si la tâche n'existe pas
     * @throws UnauthorizedAccessException si la tâche n'appartient pas à l'utilisateur authentifié et que l'utilisateur n'est pas admin
     * @throws IllegalArgumentException si le token JWT est manquant ou invalide
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Consulter une tâche par ID",
        description = "✅ Retourne une tâche par son ID si elle appartient à l'utilisateur authentifié. " +
                "🔒 Si l'utilisateur est ADMIN (ROLE_ADMIN), peut consulter toutes les tâches. " +
                "Vérifie automatiquement la propriété via le userId du token JWT. " +
                "Route: GET /api/tasks/{id} (via API Gateway)"
    )
    public ResponseEntity<TaskResponse> getTaskById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = securityUtils.getUserIdFromRequest(httpRequest);
        boolean isAdmin = securityUtils.isAdmin(httpRequest);
        TaskResponse task = taskService.getTaskById(id, userId, isAdmin);
        return ResponseEntity.ok(task);
    }
    
    /**
     * Met à jour une tâche par son ID.
     * <p>
     * Si l'utilisateur est admin (ROLE_ADMIN), peut modifier n'importe quelle tâche.
     * Sinon, vérifie que la tâche appartient bien à l'utilisateur authentifié avant de la modifier.
     * L'ID de l'utilisateur est automatiquement extrait du token JWT.
     * </p>
     *
     * @param id L'identifiant unique de la tâche à mettre à jour
     * @param request Les nouvelles informations de la tâche
     * @param httpRequest La requête HTTP pour extraire l'ID de l'utilisateur depuis le token JWT
     * @return ResponseEntity contenant la tâche mise à jour avec le statut HTTP 200 (OK)
     * @throws ResourceNotFoundException si la tâche n'existe pas
     * @throws UnauthorizedAccessException si la tâche n'appartient pas à l'utilisateur authentifié et que l'utilisateur n'est pas admin
     * @throws IllegalArgumentException si le token JWT est manquant ou invalide
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "Modifier une tâche",
        description = "✅ Modifie une tâche si elle appartient à l'utilisateur authentifié. " +
                "🔒 Si l'utilisateur est ADMIN (ROLE_ADMIN), peut modifier toutes les tâches. " +
                "Vérifie la propriété automatiquement. Publie l'événement 'task.updated' sur Kafka. " +
                "Route: PUT /api/tasks/{id} (via API Gateway)"
    )
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody TaskRequest request,
            HttpServletRequest httpRequest) {
        Long userId = securityUtils.getUserIdFromRequest(httpRequest);
        boolean isAdmin = securityUtils.isAdmin(httpRequest);
        TaskResponse task = taskService.updateTask(id, request, userId, isAdmin);
        return ResponseEntity.ok(task);
    }
    
    /**
     * Supprime une tâche par son ID.
     * <p>
     * Si l'utilisateur est admin (ROLE_ADMIN), peut supprimer n'importe quelle tâche.
     * Sinon, vérifie que la tâche appartient bien à l'utilisateur authentifié avant de la supprimer.
     * L'ID de l'utilisateur est automatiquement extrait du token JWT.
     * Cette opération est irréversible.
     * </p>
     *
     * @param id L'identifiant unique de la tâche à supprimer
     * @param httpRequest La requête HTTP pour extraire l'ID de l'utilisateur depuis le token JWT
     * @return ResponseEntity vide avec le statut HTTP 204 (No Content)
     * @throws ResourceNotFoundException si la tâche n'existe pas
     * @throws UnauthorizedAccessException si la tâche n'appartient pas à l'utilisateur authentifié et que l'utilisateur n'est pas admin
     * @throws IllegalArgumentException si le token JWT est manquant ou invalide
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Supprimer une tâche",
        description = "✅ Supprime une tâche si elle appartient à l'utilisateur authentifié. " +
                "🔒 Si l'utilisateur est ADMIN (ROLE_ADMIN), peut supprimer toutes les tâches. " +
                "Opération irréversible. Publie l'événement 'task.deleted' sur Kafka. " +
                "Route: DELETE /api/tasks/{id} (via API Gateway)"
    )
    public ResponseEntity<Void> deleteTask(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        Long userId = securityUtils.getUserIdFromRequest(httpRequest);
        boolean isAdmin = securityUtils.isAdmin(httpRequest);
        taskService.deleteTask(id, userId, isAdmin);
        return ResponseEntity.noContent().build();
    }
}

