package com.simdev.api_users.auth.controller;

import com.simdev.api_users.auth.dto.LoginRequest;
import com.simdev.api_users.auth.dto.LoginResponse;
import com.simdev.api_users.auth.dto.RegisterRequest;
import com.simdev.api_users.user.dto.UserResponse;
import com.simdev.api_users.auth.exception.BadCredentialsException;
import com.simdev.api_users.user.exception.UserAlreadyExistsException;
import com.simdev.api_users.auth.exception.UserInactiveException;
import com.simdev.api_users.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * Contrôleur REST pour la gestion de l'authentification des utilisateurs.
 * <p>
 * Ce contrôleur expose les endpoints pour l'enregistrement et la connexion des utilisateurs.
 * Tous les endpoints sont publics et ne nécessitent pas d'authentification.
 * </p>
 *
 * @author API Users Service
 * @version 1.0
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "🔐 API d'authentification - Microservice Users. " +
        "Gère l'enregistrement (register) et la connexion (login) des utilisateurs. " +
        "Les événements utilisateurs sont publiés sur Kafka pour synchronisation avec les autres microservices. " +
        "⚠️ Accessible uniquement via l'API Gateway sur /api/auth/**")
public class AuthController {
    
    private final AuthService authService;
    
    /**
     * Enregistre un nouvel utilisateur dans le système.
     * <p>
     * Crée un compte utilisateur avec le rôle ROLE_USER par défaut.
     * L'email doit être unique dans le système.
     * </p>
     *
     * @param request Les informations de l'utilisateur à enregistrer (email, mot de passe, prénom, nom)
     * @return ResponseEntity contenant les informations de l'utilisateur créé avec le statut HTTP 201 (Created)
     * @throws UserAlreadyExistsException si l'email existe déjà
     * @throws jakarta.validation.ConstraintViolationException si les données de la requête sont invalides
     */
    @PostMapping("/register")
    @Operation(
        summary = "S'enregistrer - Créer un compte utilisateur",
        description = "Crée un nouveau compte utilisateur avec le rôle ROLE_USER par défaut. " +
                "L'email doit être unique. L'événement 'user.created' est publié sur Kafka après la création. " +
                "Route: /api/auth/register (via API Gateway)"
    )
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        UserResponse user = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }
    
    /**
     * Authentifie un utilisateur et génère un token JWT.
     * <p>
     * Valide les identifiants (email et mot de passe) et retourne un token JWT
     * si l'authentification réussit. Le token contient l'ID utilisateur, l'email et le rôle.
     * </p>
     *
     * @param request Les identifiants de connexion (email et mot de passe)
     * @return ResponseEntity contenant le token JWT et les informations de l'utilisateur avec le statut HTTP 200 (OK)
     * @throws BadCredentialsException si l'email ou le mot de passe est incorrect
     * @throws UserInactiveException si le compte utilisateur est désactivé
     * @throws jakarta.validation.ConstraintViolationException si les données de la requête sont invalides
     */
    @PostMapping("/login")
    @Operation(
        summary = "Se connecter - Obtenir un token JWT",
        description = "Authentifie un utilisateur et retourne un token JWT contenant l'ID, l'email et le rôle. " +
                "Le token est requis pour accéder aux endpoints protégés. " +
                "Système de verrouillage de compte après 5 tentatives échouées (15 minutes). " +
                "Route: /api/auth/login (via API Gateway)"
    )
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }
}

