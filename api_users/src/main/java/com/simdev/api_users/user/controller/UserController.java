package com.simdev.api_users.user.controller;

import com.simdev.api_users.user.dto.UserRequest;
import com.simdev.api_users.user.dto.UserResponse;
import com.simdev.api_users.user.exception.ResourceNotFoundException;
import com.simdev.api_users.user.exception.UnauthorizedAccessException;
import com.simdev.api_users.user.exception.UserAlreadyExistsException;
import com.simdev.api_users.user.service.UserService;
import com.simdev.api_users.shared.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur REST pour la gestion des utilisateurs.
 * <p>
 * Ce contrôleur expose les endpoints pour la gestion complète des utilisateurs (CRUD).
 * La plupart des opérations nécessitent des privilèges administrateur, sauf les endpoints
 * /users/me qui permettent aux utilisateurs de gérer leur propre profil.
 * </p>
 *
 * @author API Users Service
 * @version 1.0
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "👥 API de gestion des utilisateurs - Microservice Users. " +
        "Opérations CRUD complètes sur les utilisateurs. " +
        "Seuls les administrateurs (ROLE_ADMIN) peuvent gérer tous les utilisateurs. " +
        "Les utilisateurs normaux peuvent gérer leur propre profil via /users/me. " +
        "Les modifications publient des événements sur Kafka (user.created, user.updated, user.deleted). " +
        "⚠️ Accessible uniquement via l'API Gateway sur /api/users/**")
public class UserController {
    
    private final UserService userService;
    private final SecurityUtils securityUtils;
    
    /**
     * Crée un nouvel utilisateur (réservé aux administrateurs).
     * <p>
     * Seuls les utilisateurs avec le rôle ROLE_ADMIN peuvent créer des utilisateurs via cet endpoint.
     * Les utilisateurs normaux doivent utiliser l'endpoint /auth/register pour s'enregistrer.
     * </p>
     *
     * @param request Les informations de l'utilisateur à créer
     * @param httpRequest La requête HTTP pour extraire les informations d'authentification
     * @return ResponseEntity contenant l'utilisateur créé avec le statut HTTP 201 (Created)
     * @throws UnauthorizedAccessException si l'utilisateur n'est pas admin
     * @throws UserAlreadyExistsException si l'email existe déjà
     * @throws jakarta.validation.ConstraintViolationException si les données sont invalides
     */
    @PostMapping
    @Operation(
        summary = "Créer un utilisateur (Admin uniquement)",
        description = "🔒 Requiert ROLE_ADMIN. Crée un nouvel utilisateur et publie l'événement 'user.created' sur Kafka. " +
                "Les utilisateurs normaux doivent utiliser /api/auth/register pour s'enregistrer. " +
                "Route: POST /api/users (via API Gateway)"
    )
    public ResponseEntity<UserResponse> createUser(
            @Valid @RequestBody UserRequest request,
            HttpServletRequest httpRequest) {
        if (!securityUtils.isAdmin(httpRequest)) {
            throw new UnauthorizedAccessException(
                "Seuls les administrateurs peuvent créer des utilisateurs.");
        }
        UserResponse user = userService.createUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
    
    /**
     * Récupère la liste de tous les utilisateurs (réservé aux administrateurs).
     *
     * @param httpRequest La requête HTTP pour extraire les informations d'authentification
     * @return ResponseEntity contenant la liste de tous les utilisateurs avec le statut HTTP 200 (OK)
     * @throws UnauthorizedAccessException si l'utilisateur n'est pas admin
     */
    @GetMapping
    @Operation(
        summary = "Lister tous les utilisateurs (Admin uniquement)",
        description = "🔒 Requiert ROLE_ADMIN. Retourne la liste complète de tous les utilisateurs du système. " +
                "Les utilisateurs normaux utilisent GET /api/users/me pour leur propre profil. " +
                "Route: GET /api/users (via API Gateway)"
    )
    public ResponseEntity<List<UserResponse>> getAllUsers(HttpServletRequest httpRequest) {
        if (!securityUtils.isAdmin(httpRequest)) {
            throw new UnauthorizedAccessException(
                "Seuls les administrateurs peuvent consulter la liste de tous les utilisateurs.");
        }
        List<UserResponse> users = userService.getAllUsers();
        return ResponseEntity.ok(users);
    }
    
    /**
     * Récupère le profil de l'utilisateur authentifié.
     * <p>
     * Permet à un utilisateur authentifié de consulter son propre profil.
     * Accessible à tous les utilisateurs authentifiés (ROLE_USER et ROLE_ADMIN).
     * </p>
     *
     * @param request La requête HTTP pour extraire l'ID de l'utilisateur depuis le token JWT
     * @return ResponseEntity contenant le profil de l'utilisateur avec le statut HTTP 200 (OK)
     * @throws IllegalArgumentException si le token JWT est manquant ou invalide
     * @throws ResourceNotFoundException si l'utilisateur n'existe pas
     */
    @GetMapping("/me")
    @Operation(
        summary = "Consulter mon profil",
        description = "✅ Accessible à tous les utilisateurs authentifiés. " +
                "Retourne le profil de l'utilisateur extrait automatiquement du token JWT. " +
                "Route: GET /api/users/me (via API Gateway)"
    )
    public ResponseEntity<UserResponse> getMyProfile(HttpServletRequest request) {
        Long userId = securityUtils.getUserIdFromRequest(request);
        UserResponse user = userService.getUserById(userId);
        return ResponseEntity.ok(user);
    }
    
    /**
     * Récupère un utilisateur par son ID (réservé aux administrateurs).
     * <p>
     * Seuls les administrateurs peuvent consulter le profil d'autres utilisateurs.
     * Les utilisateurs normaux doivent utiliser /users/me pour consulter leur propre profil.
     * </p>
     *
     * @param id L'identifiant unique de l'utilisateur à récupérer
     * @param httpRequest La requête HTTP pour extraire les informations d'authentification
     * @return ResponseEntity contenant l'utilisateur demandé avec le statut HTTP 200 (OK)
     * @throws UnauthorizedAccessException si l'utilisateur n'est pas admin
     * @throws ResourceNotFoundException si l'utilisateur avec l'ID spécifié n'existe pas
     */
    @GetMapping("/{id}")
    @Operation(
        summary = "Consulter un utilisateur par ID (Admin uniquement)",
        description = "🔒 Requiert ROLE_ADMIN. Consulte le profil d'un autre utilisateur par son ID. " +
                "Les utilisateurs normaux utilisent GET /api/users/me. " +
                "Route: GET /api/users/{id} (via API Gateway)"
    )
    public ResponseEntity<UserResponse> getUserById(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        if (!securityUtils.isAdmin(httpRequest)) {
            throw new UnauthorizedAccessException(
                "Seuls les administrateurs peuvent consulter le profil d'un autre utilisateur.");
        }
        UserResponse user = userService.getUserById(id);
        return ResponseEntity.ok(user);
    }
    
    /**
     * Vérifie si un utilisateur existe par son ID (endpoint interne).
     * <p>
     * Cet endpoint est utilisé par les autres microservices (comme Tasks Service)
     * pour valider l'existence d'un utilisateur avant de créer des ressources associées.
     * Cet endpoint est public et ne nécessite pas d'authentification pour permettre
     * la communication inter-services.
     * </p>
     *
     * @param id L'identifiant unique de l'utilisateur à vérifier
     * @return ResponseEntity contenant un Map avec les clés "exists" (boolean) et "userId" (Long)
     *         avec le statut HTTP 200 (OK)
     */
    @GetMapping("/{id}/exists")
    @Operation(summary = "Check if user exists (internal endpoint)", hidden = true)
    public ResponseEntity<Map<String, Object>> checkUserExists(@PathVariable Long id) {
        boolean exists = userService.userExists(id);
        Map<String, Object> response = new HashMap<>();
        response.put("exists", exists);
        response.put("userId", id);
        return ResponseEntity.ok(response);
    }
    
    /**
     * Met à jour le profil de l'utilisateur authentifié.
     * <p>
     * Permet à un utilisateur authentifié de modifier son propre profil
     * (prénom, nom, mot de passe). Le mot de passe est optionnel et n'est mis à jour
     * que s'il est fourni et non vide.
     * </p>
     *
     * @param request Les nouvelles informations de l'utilisateur
     * @param httpRequest La requête HTTP pour extraire l'ID de l'utilisateur depuis le token JWT
     * @return ResponseEntity contenant l'utilisateur mis à jour avec le statut HTTP 200 (OK)
     * @throws IllegalArgumentException si le token JWT est manquant ou invalide
     * @throws ResourceNotFoundException si l'utilisateur n'existe pas
     * @throws jakarta.validation.ConstraintViolationException si les données sont invalides
     */
    @PutMapping("/me")
    @Operation(
        summary = "Modifier mon profil",
        description = "✅ Accessible à tous les utilisateurs authentifiés. " +
                "Met à jour le profil de l'utilisateur authentifié et publie l'événement 'user.updated' sur Kafka. " +
                "Route: PUT /api/users/me (via API Gateway)"
    )
    public ResponseEntity<UserResponse> updateMyProfile(
            @Valid @RequestBody UserRequest request,
            HttpServletRequest httpRequest) {
        Long userId = securityUtils.getUserIdFromRequest(httpRequest);
        UserResponse user = userService.updateUser(userId, request);
        return ResponseEntity.ok(user);
    }
    
    /**
     * Met à jour un utilisateur par son ID (réservé aux administrateurs).
     * <p>
     * Seuls les administrateurs peuvent modifier le profil d'autres utilisateurs.
     * Les utilisateurs normaux doivent utiliser /users/me pour modifier leur propre profil.
     * </p>
     *
     * @param id L'identifiant unique de l'utilisateur à mettre à jour
     * @param request Les nouvelles informations de l'utilisateur
     * @param httpRequest La requête HTTP pour extraire les informations d'authentification
     * @return ResponseEntity contenant l'utilisateur mis à jour avec le statut HTTP 200 (OK)
     * @throws UnauthorizedAccessException si l'utilisateur n'est pas admin
     * @throws ResourceNotFoundException si l'utilisateur avec l'ID spécifié n'existe pas
     * @throws jakarta.validation.ConstraintViolationException si les données sont invalides
     */
    @PutMapping("/{id}")
    @Operation(
        summary = "Modifier un utilisateur (Admin uniquement)",
        description = "🔒 Requiert ROLE_ADMIN. Modifie le profil d'un autre utilisateur et publie l'événement 'user.updated' sur Kafka. " +
                "Les utilisateurs normaux utilisent PUT /api/users/me. " +
                "Route: PUT /api/users/{id} (via API Gateway)"
    )
    public ResponseEntity<UserResponse> updateUser(
            @PathVariable Long id,
            @Valid @RequestBody UserRequest request,
            HttpServletRequest httpRequest) {
        if (!securityUtils.isAdmin(httpRequest)) {
            throw new UnauthorizedAccessException(
                "Seuls les administrateurs peuvent modifier le profil d'un autre utilisateur.");
        }
        
        UserResponse user = userService.updateUser(id, request);
        return ResponseEntity.ok(user);
    }
    
    /**
     * Supprime le compte de l'utilisateur authentifié.
     * <p>
     * Permet à un utilisateur authentifié de supprimer son propre compte.
     * Cette opération est irréversible.
     * </p>
     *
     * @param httpRequest La requête HTTP pour extraire l'ID de l'utilisateur depuis le token JWT
     * @return ResponseEntity vide avec le statut HTTP 204 (No Content)
     * @throws IllegalArgumentException si le token JWT est manquant ou invalide
     * @throws ResourceNotFoundException si l'utilisateur n'existe pas
     */
    @DeleteMapping("/me")
    @Operation(
        summary = "Supprimer mon compte",
        description = "✅ Accessible à tous les utilisateurs authentifiés. " +
                "Supprime le compte de l'utilisateur authentifié et publie l'événement 'user.deleted' sur Kafka. " +
                "Les tâches associées seront également supprimées par le service Tasks (via événement Kafka). " +
                "Route: DELETE /api/users/me (via API Gateway)"
    )
    public ResponseEntity<Void> deleteMyAccount(HttpServletRequest httpRequest) {
        Long userId = securityUtils.getUserIdFromRequest(httpRequest);
        userService.deleteUser(userId);
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Supprime un utilisateur par son ID (réservé aux administrateurs).
     * <p>
     * Seuls les administrateurs peuvent supprimer le compte d'autres utilisateurs.
     * Les utilisateurs normaux peuvent utiliser /users/me pour supprimer leur propre compte.
     * Cette opération est irréversible.
     * </p>
     *
     * @param id L'identifiant unique de l'utilisateur à supprimer
     * @param httpRequest La requête HTTP pour extraire les informations d'authentification
     * @return ResponseEntity vide avec le statut HTTP 204 (No Content)
     * @throws UnauthorizedAccessException si l'utilisateur n'est pas admin
     * @throws ResourceNotFoundException si l'utilisateur avec l'ID spécifié n'existe pas
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Supprimer un utilisateur (Admin uniquement)",
        description = "🔒 Requiert ROLE_ADMIN. Supprime le compte d'un utilisateur et publie l'événement 'user.deleted' sur Kafka. " +
                "Les tâches associées seront automatiquement supprimées par le service Tasks (écoute Kafka). " +
                "Route: DELETE /api/users/{id} (via API Gateway)"
    )
    public ResponseEntity<Void> deleteUser(
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        if (!securityUtils.isAdmin(httpRequest)) {
            throw new UnauthorizedAccessException(
                "Seuls les administrateurs peuvent supprimer le compte d'un autre utilisateur.");
        }
        
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }
}

