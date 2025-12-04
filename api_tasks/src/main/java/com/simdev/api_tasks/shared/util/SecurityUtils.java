package com.simdev.api_tasks.shared.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

/**
 * Utilitaire pour extraire les informations du JWT token depuis les requêtes HTTP.
 * <p>
 * Ce composant fournit des méthodes pour extraire l'ID utilisateur depuis le token JWT
 * présent dans le header Authorization des requêtes HTTP.
 * </p>
 *
 * @author API Tasks Service
 * @version 1.0
 */
@Component
@Slf4j
public class SecurityUtils {
    
    @Value("${jwt.secret:your_super_secret_jwt_key_that_is_at_least_256_bits_long_for_HS256_algorithm}")
    private String jwtSecret;
    
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
        return extractUserIdFromToken(token);
    }
    
    /**
     * Extrait l'identifiant unique de l'utilisateur depuis un token JWT.
     *
     * @param token Le token JWT à parser
     * @return L'identifiant unique de l'utilisateur
     * @throws IllegalArgumentException si le token est invalide, expiré ou ne contient pas d'userId
     */
    public Long extractUserIdFromToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Object userId = claims.get("userId");
            if (userId instanceof Integer) {
                return ((Integer) userId).longValue();
            }
            if (userId instanceof Long) {
                return (Long) userId;
            }
            log.error("Token JWT valide mais userId manquant ou de type invalide: {}", userId != null ? userId.getClass().getSimpleName() : "null");
            throw new IllegalArgumentException("Token JWT invalide : identifiant utilisateur manquant");
        } catch (IllegalArgumentException e) {
            // Re-lancer les IllegalArgumentException sans modification
            throw e;
        } catch (Exception e) {
            log.error("Erreur lors de l'extraction de l'userId depuis le token: {} - Type: {}", e.getMessage(), e.getClass().getSimpleName(), e);
            throw new IllegalArgumentException("Token JWT invalide ou expiré. Veuillez vous reconnecter pour obtenir un nouveau token.");
        }
    }
    
    /**
     * Extrait le rôle de l'utilisateur depuis le token JWT.
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
        return extractRoleFromToken(token);
    }
    
    /**
     * Extrait le rôle de l'utilisateur depuis un token JWT.
     *
     * @param token Le token JWT à parser
     * @return Le rôle de l'utilisateur (ROLE_USER ou ROLE_ADMIN)
     */
    public String extractRoleFromToken(String token) {
        try {
            Claims claims = extractAllClaims(token);
            Object role = claims.get("role");
            if (role != null) {
                return role.toString();
            }
            return "ROLE_USER";
        } catch (Exception e) {
            log.error("Erreur lors de l'extraction du rôle depuis le token: {}", e.getMessage());
            return "ROLE_USER";
        }
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
            log.error("Erreur lors de la vérification du rôle admin: {}", e.getMessage());
            return false;
        }
    }
    
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            log.error("Token JWT expiré: {}", e.getMessage());
            throw new IllegalArgumentException("Token JWT expiré. Veuillez vous reconnecter pour obtenir un nouveau token.");
        } catch (io.jsonwebtoken.security.SignatureException e) {
            log.error("Signature JWT invalide. Secret utilisé: {} (longueur: {})", 
                jwtSecret != null ? jwtSecret.substring(0, Math.min(10, jwtSecret.length())) + "..." : "null",
                jwtSecret != null ? jwtSecret.length() : 0);
            throw new IllegalArgumentException("Token JWT invalide : signature incorrecte. Le secret JWT ne correspond pas.");
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            log.error("Token JWT malformé: {}", e.getMessage());
            throw new IllegalArgumentException("Token JWT invalide : format incorrect.");
        } catch (Exception e) {
            log.error("Erreur lors du parsing du token JWT: {} - Type: {}", e.getMessage(), e.getClass().getSimpleName());
            throw new IllegalArgumentException("Token JWT invalide ou expiré. Veuillez vous reconnecter pour obtenir un nouveau token.");
        }
    }
    
    /**
     * Génère la clé de signature pour JWT.
     * <p>
     * Utilise EXACTEMENT la même logique que JwtService dans api_users pour garantir la compatibilité
     * des tokens entre les services. Si le secret fait moins de 32 caractères, il est répété
     * et tronqué à 32 caractères pour respecter les exigences de l'algorithme HS256.
     * </p>
     * <p>
     * IMPORTANT: Utilise getBytes() sans charset pour correspondre exactement à api_users.
     * </p>
     *
     * @return La clé secrète pour signer/vérifier les tokens JWT
     */
    private SecretKey getSigningKey() {
        String key = jwtSecret;
        
        // Si la clé est trop courte, la répéter jusqu'à avoir au moins 32 caractères
        if (key.length() < 32) {
            int repetitions = (32 / key.length()) + 1;
            key = key.repeat(repetitions);
        }
        
        // Prendre exactement 32 caractères - utiliser getBytes() sans charset comme dans api_users
        byte[] keyBytes = key.substring(0, Math.min(32, key.length())).getBytes();
        
        // Si les bytes sont toujours trop courts, les compléter
        if (keyBytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(keyBytes, 0, padded, 0, keyBytes.length);
            for (int i = keyBytes.length; i < 32; i++) {
                padded[i] = keyBytes[i % keyBytes.length];
            }
            keyBytes = padded;
        }
        
        return Keys.hmacShaKeyFor(keyBytes);
    }
}

