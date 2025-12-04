package com.simdev.api_users.shared.util;

import com.simdev.api_users.auth.service.JwtService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Utilitaire pour extraire les informations du JWT token depuis les requêtes HTTP.
 * <p>
 * Ce composant fournit des méthodes pratiques pour extraire et manipuler les informations
 * d'authentification depuis le token JWT présent dans le header {@code Authorization}
 * des requêtes HTTP. Il simplifie l'accès aux informations de l'utilisateur authentifié
 * dans les contrôleurs et services.
 * </p>
 * <p>
 * <strong>Fonctionnalités :</strong>
 * <ul>
 *   <li><strong>Extraction d'ID :</strong> Récupère l'identifiant unique de l'utilisateur</li>
 *   <li><strong>Extraction d'email :</strong> Récupère l'email de l'utilisateur</li>
 *   <li><strong>Extraction de rôle :</strong> Récupère le rôle (ROLE_USER ou ROLE_ADMIN)</li>
 *   <li><strong>Vérification admin :</strong> Méthode utilitaire pour vérifier si l'utilisateur est admin</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Format du header Authorization :</strong>
 * <pre>
 * Authorization: Bearer &lt;jwt-token&gt;
 * </pre>
 * </p>
 * <p>
 * <strong>Gestion des erreurs :</strong>
 * Si le header Authorization est manquant ou invalide, toutes les méthodes lèvent
 * une {@link IllegalArgumentException} avec un message explicite.
 * </p>
 * <p>
 * <strong>Utilisation typique :</strong>
 * <pre>
 * // Dans un contrôleur
 * Long userId = securityUtils.getUserIdFromRequest(request);
 * boolean isAdmin = securityUtils.isAdmin(request);
 * </pre>
 * </p>
 *
 * @author API Users Service
 * @version 1.0
 * @see JwtService
 * @see HttpServletRequest
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class SecurityUtils {
    
    private final JwtService jwtService;
    
    /**
     * Extrait l'identifiant unique de l'utilisateur depuis le token JWT dans le header Authorization.
     *
     * @param request La requête HTTP contenant le header Authorization
     * @return L'identifiant unique de l'utilisateur
     * @throws IllegalArgumentException si le header Authorization est manquant ou invalide
     */
    public Long getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authentification requise. Veuillez fournir un token JWT valide dans le header Authorization.");
        }
        
        String token = authHeader.substring(7);
        return jwtService.extractUserId(token);
    }
    
    /**
     * Extrait l'email de l'utilisateur depuis le token JWT dans le header Authorization.
     *
     * @param request La requête HTTP contenant le header Authorization
     * @return L'email de l'utilisateur
     * @throws IllegalArgumentException si le header Authorization est manquant ou invalide
     */
    public String getEmailFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authentification requise. Veuillez fournir un token JWT valide dans le header Authorization.");
        }
        
        String token = authHeader.substring(7);
        return jwtService.extractEmail(token);
    }
    
    /**
     * Extrait le rôle de l'utilisateur depuis le token JWT dans le header Authorization.
     *
     * @param request La requête HTTP contenant le header Authorization
     * @return Le rôle de l'utilisateur (ROLE_USER ou ROLE_ADMIN)
     * @throws IllegalArgumentException si le header Authorization est manquant ou invalide
     */
    public String getRoleFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new IllegalArgumentException("Authentification requise. Veuillez fournir un token JWT valide dans le header Authorization.");
        }
        
        String token = authHeader.substring(7);
        return jwtService.extractRole(token);
    }
    
    /**
     * Vérifie si l'utilisateur authentifié possède le rôle administrateur.
     *
     * @param request La requête HTTP contenant le header Authorization
     * @return true si l'utilisateur est admin (ROLE_ADMIN), false sinon
     */
    public boolean isAdmin(HttpServletRequest request) {
        try {
            String role = getRoleFromRequest(request);
            return "ROLE_ADMIN".equals(role);
        } catch (Exception e) {
            return false;
        }
    }
}

