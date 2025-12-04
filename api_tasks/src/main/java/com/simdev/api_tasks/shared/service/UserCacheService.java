package com.simdev.api_tasks.shared.service;

import com.simdev.api_tasks.shared.exception.UserNotFoundException;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service de cache et validation de l'existence des utilisateurs (résilience et performance).
 * <p>
 * Ce service maintient un cache en mémoire des utilisateurs pour optimiser les performances
 * et réduire la charge sur le service Users. Il intègre des mécanismes de résilience
 * pour garantir la disponibilité même en cas de panne du service Users.
 * </p>
 * <p>
 * <strong>Fonctionnalités :</strong>
 * <ul>
 *   <li><strong>Cache en mémoire :</strong> Stocke les validations d'utilisateurs avec TTL configurable</li>
 *   <li><strong>Circuit Breaker :</strong> Protège contre les pannes du service Users (Resilience4j)</li>
 *   <li><strong>Retry automatique :</strong> Réessaie automatiquement en cas d'échec temporaire</li>
 *   <li><strong>Fallback :</strong> Utilise le cache étendu en cas d'ouverture du circuit breaker</li>
 *   <li><strong>Invalidation :</strong> Permet de vider le cache lors des mises à jour d'utilisateurs</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Stratégie de cache :</strong>
 * <ul>
 *   <li>TTL par défaut : 300 secondes (5 minutes) - configurable via {@code cache.user.ttl-seconds}</li>
 *   <li>Vérification d'expiration à chaque accès</li>
 *   <li>Mise à jour automatique lors des appels au service</li>
 *   <li>Invalidation manuelle via {@code evictUser()} et {@code evictAllUsers()}</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Résilience (Resilience4j) :</strong>
 * <ul>
 *   <li><strong>Circuit Breaker :</strong> S'ouvre après un seuil d'échecs pour éviter la surcharge</li>
 *   <li><strong>Fallback :</strong> En cas d'ouverture, utilise le cache avec TTL doublé ou assume l'existence</li>
 *   <li><strong>Retry :</strong> Réessaie automatiquement les appels avec backoff exponentiel</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Utilisation :</strong>
 * Ce service est utilisé par {@link TaskService} pour valider l'existence d'un utilisateur
 * avant de créer une tâche, évitant ainsi les références orphelines.
 * </p>
 *
 * @author API Tasks Service
 * @version 1.0
 * @see TaskService
 * @see RestTemplate
 */
@Service
@Slf4j
public class UserCacheService {
    
    private final RestTemplate restTemplate;
    private final Map<Long, CacheEntry> userCache = new ConcurrentHashMap<>();
    
    @Value("${services.users.url:http://api-users:8081}")
    private String usersServiceUrl;
    
    @Value("${cache.user.ttl-seconds:300}")
    private long cacheTtlSeconds;
    
    public UserCacheService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }
    
    /**
     * Vérifie si un utilisateur existe par son identifiant unique.
     * <p>
     * Cette méthode utilise un cache en mémoire pour améliorer les performances.
     * Si l'utilisateur n'est pas en cache ou si l'entrée est expirée, un appel
     * est fait au service Users avec circuit breaker et retry logic.
     * </p>
     *
     * @param userId L'identifiant unique de l'utilisateur à vérifier
     * @return true si l'utilisateur existe, false sinon
     * @throws UserNotFoundException si l'utilisateur n'existe pas
     */
    @CircuitBreaker(name = "userService", fallbackMethod = "userExistsFallback")
    @Retry(name = "userService")
    public boolean userExists(Long userId) {
        CacheEntry cachedEntry = userCache.get(userId);
        if (cachedEntry != null && !cachedEntry.isExpired(cacheTtlSeconds)) {
            log.debug("User {} found in cache", userId);
            if (!cachedEntry.exists) {
                throw new UserNotFoundException(userId);
            }
            return true;
        }
        
        boolean exists = checkUserExistsFromService(userId);
        userCache.put(userId, new CacheEntry(exists, LocalDateTime.now()));
        
        if (!exists) {
            throw new UserNotFoundException(userId);
        }
        
        log.debug("User {} validated and cached", userId);
        return true;
    }
    
    private boolean checkUserExistsFromService(Long userId) {
        try {
            String url = usersServiceUrl + "/users/" + userId + "/exists";
            @SuppressWarnings("rawtypes")
            ResponseEntity<Map> response = restTemplate.getForEntity(url, Map.class);
            
            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> body = response.getBody();
                if (body != null) {
                    Boolean exists = (Boolean) body.get("exists");
                    return Boolean.TRUE.equals(exists);
                }
            }
            return false;
        } catch (RestClientException e) {
            log.warn("Error checking user {} existence: {}", userId, e.getMessage());
            return false;
        }
    }
    
    @SuppressWarnings("unused")
    private boolean userExistsFallback(Long userId, Exception ex) {
        log.warn("Circuit breaker opened for user service, using cache for userId: {}", userId);
        CacheEntry entry = userCache.get(userId);
        if (entry != null && !entry.isExpired(cacheTtlSeconds * 2)) {
            return entry.exists;
        }
        log.warn("No cache entry for userId: {}, assuming user exists in fallback", userId);
        return true;
    }
    
    /**
     * Supprime un utilisateur du cache.
     *
     * @param userId L'identifiant unique de l'utilisateur à retirer du cache
     */
    public void evictUser(Long userId) {
        userCache.remove(userId);
        log.debug("User {} evicted from cache", userId);
    }
    
    /**
     * Vide complètement le cache des utilisateurs.
     */
    public void evictAllUsers() {
        userCache.clear();
        log.debug("All users evicted from cache");
    }
    
    private static class CacheEntry {
        final boolean exists;
        final LocalDateTime timestamp;
        
        CacheEntry(boolean exists, LocalDateTime timestamp) {
            this.exists = exists;
            this.timestamp = timestamp;
        }
        
        boolean isExpired(long ttlSeconds) {
            return Duration.between(timestamp, LocalDateTime.now()).getSeconds() > ttlSeconds;
        }
    }
}

