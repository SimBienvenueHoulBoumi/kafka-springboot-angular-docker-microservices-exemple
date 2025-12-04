package com.simdev.api_users.shared.config;

import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Configuration du rate limiting (limitation de débit) pour l'API.
 * <p>
 * Cette configuration protège l'API contre les abus et les attaques par déni de service (DDoS)
 * en limitant le nombre de requêtes qu'un client peut faire dans un intervalle de temps donné.
 * </p>
 * <p>
 * <strong>Fonctionnalités :</strong>
 * <ul>
 *   <li><strong>Cache en mémoire :</strong> Utilise un ConcurrentMapCacheManager pour stocker
 *       les compteurs de requêtes par adresse IP</li>
 *   <li><strong>Interceptor :</strong> {@link RateLimitingInterceptor} intercepte les requêtes
 *       avant qu'elles n'atteignent les contrôleurs</li>
 *   <li><strong>Endpoints protégés :</strong> Rate limiting appliqué sur les endpoints sensibles</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Endpoints protégés :</strong>
 * <ul>
 *   <li>{@code /auth/login} - Protection contre les attaques par force brute</li>
 *   <li>{@code /auth/register} - Protection contre les inscriptions abusives</li>
 *   <li>{@code /users/**} - Protection générale des endpoints utilisateurs</li>
 *   <li>{@code /tasks/**} - Protection générale des endpoints tâches</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Endpoints exclus :</strong>
 * <ul>
 *   <li>{@code /actuator/health} - Health check doit rester accessible</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Configuration :</strong>
 * Les limites de débit (nombre de requêtes par minute, etc.) sont configurées dans
 * {@link RateLimitingInterceptor}. Par défaut, une limite de 100 requêtes par minute
 * est appliquée par adresse IP.
 * </p>
 *
 * @author API Users Service
 * @version 1.0
 * @see RateLimitingInterceptor
 * @see WebMvcConfigurer
 */
@Configuration
public class RateLimitingConfig implements WebMvcConfigurer {
    
    @Bean
    public CacheManager rateLimitCacheManager() {
        return new ConcurrentMapCacheManager("rateLimits");
    }
    
    @Bean
    public RateLimitingInterceptor rateLimitingInterceptor() {
        return new RateLimitingInterceptor(rateLimitCacheManager());
    }
    
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitingInterceptor())
                .addPathPatterns("/auth/login", "/auth/register")
                .addPathPatterns("/users/**", "/tasks/**")
                .excludePathPatterns("/actuator/health");
    }
}

