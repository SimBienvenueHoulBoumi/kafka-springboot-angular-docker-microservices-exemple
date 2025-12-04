package com.simdev.api_users.auth.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Service de gestion des tokens JWT (JSON Web Tokens).
 * <p>
 * Ce service gère la génération, la validation et l'extraction d'informations
 * depuis les tokens JWT utilisés pour l'authentification.
 * </p>
 *
 * @author API Users Service
 * @version 1.0
 */
@Service
@Slf4j
public class JwtService {
    
    @Value("${jwt.secret:your_super_secret_jwt_key_that_is_at_least_256_bits_long_for_HS256_algorithm}")
    private String secret;
    
    @Value("${jwt.expiration:86400000}")
    private long expiration;
    
    private SecretKey getSigningKey() {
        String key = secret;
        if (key.length() < 32) {
            int repetitions = (32 / key.length()) + 1;
            key = key.repeat(repetitions);
        }
        byte[] keyBytes = key.substring(0, Math.min(32, key.length())).getBytes();
        
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
    
    /**
     * Extrait l'email de l'utilisateur depuis un token JWT.
     *
     * @param token Le token JWT
     * @return L'email de l'utilisateur (subject du token)
     */
    public String extractEmail(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    /**
     * Extrait l'identifiant unique de l'utilisateur depuis un token JWT.
     *
     * @param token Le token JWT
     * @return L'identifiant unique de l'utilisateur
     */
    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        Object userId = claims.get("userId");
        if (userId instanceof Integer) {
            return ((Integer) userId).longValue();
        }
        return claims.get("userId", Long.class);
    }
    
    /**
     * Extrait le rôle de l'utilisateur depuis un token JWT.
     *
     * @param token Le token JWT
     * @return Le rôle de l'utilisateur (ROLE_USER ou ROLE_ADMIN)
     */
    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }
    
    /**
     * Extrait la date d'expiration depuis un token JWT.
     *
     * @param token Le token JWT
     * @return La date d'expiration du token
     */
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    /**
     * Extrait une claim spécifique depuis un token JWT.
     *
     * @param token Le token JWT
     * @param claimsResolver La fonction pour extraire la claim souhaitée
     * @param <T> Le type de la claim à extraire
     * @return La valeur de la claim extraite
     */
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    private Claims extractAllClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (Exception e) {
            log.error("Error parsing JWT token: {}", e.getMessage());
            throw e;
        }
    }
    
    private Boolean isTokenExpired(String token) {
        try {
            return extractExpiration(token).before(new Date());
        } catch (Exception e) {
            return true;
        }
    }
    
    /**
     * Génère un token JWT pour un utilisateur.
     * <p>
     * Le token contient l'ID utilisateur, l'email (subject) et le rôle.
     * </p>
     *
     * @param userDetails Les détails de l'utilisateur (contient l'email et le rôle)
     * @param userId L'identifiant unique de l'utilisateur
     * @return Le token JWT généré
     */
    public String generateToken(UserDetails userDetails, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        if (userDetails.getAuthorities() != null && !userDetails.getAuthorities().isEmpty()) {
            String role = userDetails.getAuthorities().iterator().next().getAuthority();
            claims.put("role", role);
        }
        return createToken(claims, userDetails.getUsername());
    }
    
    private String createToken(Map<String, Object> claims, String subject) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);
        
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }
    
    /**
     * Valide un token JWT en vérifiant qu'il correspond à l'utilisateur et qu'il n'est pas expiré.
     *
     * @param token Le token JWT à valider
     * @param userDetails Les détails de l'utilisateur pour comparaison
     * @return true si le token est valide, false sinon
     */
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String email = extractEmail(token);
            return (email.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
    
    /**
     * Valide un token JWT en vérifiant qu'il n'est pas expiré.
     *
     * @param token Le token JWT à valider
     * @return true si le token n'est pas expiré, false sinon
     */
    public Boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.error("Token validation failed: {}", e.getMessage());
            return false;
        }
    }
}

