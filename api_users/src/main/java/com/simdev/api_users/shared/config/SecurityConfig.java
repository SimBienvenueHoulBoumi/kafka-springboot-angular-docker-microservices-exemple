package com.simdev.api_users.shared.config;

import com.simdev.api_users.shared.security.JwtAuthenticationFilter;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Configuration de sécurité Spring Security pour le microservice Users.
 * <p>
 * Cette configuration définit :
 * <ul>
 *   <li><strong>Authentification stateless :</strong> Utilisation de JWT (pas de sessions HTTP)</li>
 *   <li><strong>Filtre JWT :</strong> Validation automatique des tokens JWT sur chaque requête</li>
 *   <li><strong>Encodage BCrypt :</strong> Hachage sécurisé des mots de passe</li>
 *   <li><strong>Routes publiques :</strong> Endpoints d'authentification accessibles sans token</li>
 *   <li><strong>RBAC :</strong> Contrôle d'accès basé sur les rôles (ROLE_USER, ROLE_ADMIN)</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Endpoints publics (sans authentification) :</strong>
 * <ul>
 *   <li>{@code /auth/**} - Inscription et connexion</li>
 *   <li>{@code /v3/api-docs/**, /swagger-ui/**} - Documentation API</li>
 *   <li>{@code /actuator/health} - Health check</li>
 *   <li>{@code /users/{id}/exists} - Validation inter-services</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Endpoints protégés :</strong>
 * <ul>
 *   <li>{@code /users/me} - Gestion de son propre profil (authentification requise)</li>
 *   <li>{@code /users/**} - Administration des utilisateurs (ROLE_ADMIN requis)</li>
 *   <li>{@code /actuator/**} - Métriques et monitoring (ROLE_ADMIN requis)</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Important :</strong>
 * CORS est complètement désactivé car il est géré par l'API Gateway.
 * Ce microservice n'est pas accessible directement depuis le frontend,
 * toutes les requêtes passent par l'API Gateway qui gère CORS et le routage.
 * </p>
 *
 * @author API Users Service
 * @version 1.0
 * @see JwtAuthenticationFilter
 * @see UserDetailsService
 */
@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    
    private final JwtAuthenticationFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;
    
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
    
    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }
    
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
    
    /**
     * CORS complètement désactivé car il est géré par l'API Gateway.
     * <p>
     * Les requêtes passent uniquement par l'API Gateway (port 8080) qui gère CORS.
     * Le service api_users (port 8081) n'est pas accessible directement depuis le frontend.
     * </p>
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.disable()) // Désactiver complètement CORS car géré par l'API Gateway
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentTypeOptions(contentType -> {})
            )
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .requestMatchers("/actuator/health").permitAll()
                .requestMatchers("/actuator/**").hasRole("ADMIN")
                .requestMatchers("/users/*/exists").permitAll()
                .requestMatchers("/users/me").authenticated()
                .requestMatchers("/users/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .authenticationProvider(authenticationProvider())
            .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);
        
        return http.build();
    }
}

