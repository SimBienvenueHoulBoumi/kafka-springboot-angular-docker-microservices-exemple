package com.simdev.api_users.shared.security;

import com.simdev.api_users.auth.service.JwtService;
import com.simdev.api_users.auth.service.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Filtre Spring Security pour l'authentification JWT.
 * <p>
 * Ce filtre intercepte toutes les requêtes HTTP et valide automatiquement
 * les tokens JWT présents dans le header {@code Authorization: Bearer <token>}.
 * </p>
 * <p>
 * <strong>Fonctionnement :</strong>
 * <ol>
 *   <li>Extrait le token JWT du header Authorization (format: "Bearer &lt;token&gt;")</li>
 *   <li>Valide la signature et l'expiration du token via {@link JwtService}</li>
 *   <li>Charge les détails de l'utilisateur depuis la base de données</li>
 *   <li>Crée une authentification Spring Security et l'ajoute au contexte</li>
 *   <li>Permet aux endpoints protégés d'accéder aux informations de l'utilisateur authentifié</li>
 * </ol>
 * </p>
 * <p>
 * <strong>Gestion des erreurs :</strong>
 * En cas d'erreur (token invalide, utilisateur inexistant, etc.), le filtre
 * laisse simplement passer la requête sans authentification. Les endpoints
 * protégés rejetteront automatiquement la requête si l'authentification est requise.
 * </p>
 * <p>
 * <strong>Performance :</strong>
 * Le filtre vérifie d'abord si une authentification existe déjà dans le contexte
 * pour éviter les opérations redondantes sur la même requête.
 * </p>
 *
 * @author API Users Service
 * @version 1.0
 * @see JwtService
 * @see UserDetailsServiceImpl
 * @see OncePerRequestFilter
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    
    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    
    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain
    ) throws ServletException, IOException {
        
        final String authHeader = request.getHeader("Authorization");
        final String jwt;
        final String email;
        
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }
        
        jwt = authHeader.substring(7);
        
        try {
            if (!jwtService.validateToken(jwt)) {
                log.warn("Invalid or expired JWT token");
                filterChain.doFilter(request, response);
                return;
            }
            
            email = jwtService.extractEmail(jwt);
            
            if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                try {
                    UserDetails userDetails = userDetailsService.loadUserByUsername(email);
                    
                    if (jwtService.validateToken(jwt, userDetails)) {
                        UsernamePasswordAuthenticationToken authToken = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );
                        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        
                        SecurityContextHolder.getContext().setAuthentication(authToken);
                        
                        log.debug("User authenticated: {}", email);
                    } else {
                        log.warn("Token validation failed for user: {}", email);
                    }
                } catch (UsernameNotFoundException e) {
                    log.warn("User not found for email in token: {}", email);
                }
            }
        } catch (Exception e) {
            log.error("Error processing JWT token: {} - {}", e.getClass().getSimpleName(), e.getMessage(), e);
        }
        
        filterChain.doFilter(request, response);
    }
}

