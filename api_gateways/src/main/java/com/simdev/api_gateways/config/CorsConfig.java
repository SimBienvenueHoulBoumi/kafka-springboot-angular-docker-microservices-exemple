package com.simdev.api_gateways.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration CORS pour l'API Gateway.
 * <p>
 * Cette configuration permet aux frontends Angular (ports 3000 et 4200)
 * d'accéder aux APIs du backend via l'API Gateway.
 * </p>
 *
 * @author API Gateway
 * @version 1.0
 */
@Configuration
public class CorsConfig {
    
    /**
     * Configure le filtre CORS pour Spring Cloud Gateway.
     * <p>
     * Autorise :
     * - Les origines : http://localhost:3000 et http://localhost:4200
     * - Les méthodes HTTP : GET, POST, PUT, DELETE, PATCH, OPTIONS
     * - Tous les headers
     * - Les credentials (cookies, tokens d'authentification)
     * </p>
     *
     * @return Le filtre CORS configuré pour WebFlux
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // Origines autorisées (les deux frontends Angular)
        corsConfig.setAllowedOrigins(Arrays.asList(
                "http://localhost:3000",
                "http://localhost:4200"
        ));
        
        // Méthodes HTTP autorisées
        corsConfig.setAllowedMethods(Arrays.asList(
                "GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"
        ));
        
        // Tous les headers autorisés (pour plus de flexibilité)
        corsConfig.setAllowedHeaders(List.of("*"));
        
        // Headers exposés au client
        corsConfig.setExposedHeaders(Arrays.asList(
                "Authorization",
                "Content-Type",
                "Access-Control-Allow-Origin",
                "Access-Control-Allow-Credentials",
                "X-Total-Count"
        ));
        
        // Autoriser l'envoi de credentials (cookies, tokens)
        corsConfig.setAllowCredentials(true);
        
        // Durée de mise en cache de la réponse preflight (en secondes)
        corsConfig.setMaxAge(3600L);
        
        // Configuration de la source des URLs CORS
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);
        
        return new CorsWebFilter(source);
    }
}

