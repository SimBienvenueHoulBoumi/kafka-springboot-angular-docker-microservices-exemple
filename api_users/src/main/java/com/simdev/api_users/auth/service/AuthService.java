package com.simdev.api_users.auth.service;

import com.simdev.api_users.user.domain.Role;
import com.simdev.api_users.user.domain.User;
import com.simdev.api_users.auth.dto.LoginRequest;
import com.simdev.api_users.auth.dto.LoginResponse;
import com.simdev.api_users.auth.dto.RegisterRequest;
import com.simdev.api_users.user.dto.UserResponse;
import com.simdev.api_users.auth.exception.AccountLockedException;
import com.simdev.api_users.auth.exception.BadCredentialsException;
import com.simdev.api_users.user.exception.UserAlreadyExistsException;
import com.simdev.api_users.auth.exception.UserInactiveException;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import com.simdev.api_users.user.repository.UserRepository;
import com.simdev.api_users.shared.metrics.CustomMetrics;
import com.simdev.api_users.shared.util.LoggingUtils;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service d'authentification et d'enregistrement des utilisateurs.
 * <p>
 * Ce service gère l'enregistrement de nouveaux utilisateurs et l'authentification
 * des utilisateurs existants via la génération de tokens JWT.
 * </p>
 * <p>
 * <strong>Fonctionnalités principales :</strong>
 * <ul>
 *   <li><strong>Enregistrement :</strong> Création de nouveaux comptes utilisateurs avec validation
 *       de l'unicité de l'email et hachage BCrypt du mot de passe</li>
 *   <li><strong>Authentification :</strong> Validation des identifiants et génération de tokens JWT</li>
 *   <li><strong>Sécurité renforcée :</strong> Système de verrouillage de compte après échecs répétés</li>
 *   <li><strong>Métriques :</strong> Suivi des performances et statistiques d'authentification</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Système de verrouillage de compte :</strong>
 * Après 5 tentatives de connexion échouées consécutives, le compte est verrouillé
 * pour une durée de 15 minutes. Le verrouillage est automatiquement levé après
 * cette période ou lors d'une connexion réussie.
 * </p>
 * <p>
 * <strong>Gestion des métriques :</strong>
 * Le service enregistre automatiquement :
 * <ul>
 *   <li>Durée des opérations d'inscription et de connexion</li>
 *   <li>Nombre d'inscriptions réussies</li>
 *   <li>Nombre de connexions réussies</li>
 *   <li>Nombre d'échecs d'authentification</li>
 * </ul>
 * Ces métriques sont accessibles via Actuator pour le monitoring.
 * </p>
 *
 * @author API Users Service
 * @version 1.0
 * @see JwtService
 * @see UserDetailsServiceImpl
 * @see CustomMetrics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserDetailsServiceImpl userDetailsService;
    private final CustomMetrics customMetrics;
    
    private static final int MAX_LOGIN_ATTEMPTS = 5;
    private static final long LOCKOUT_DURATION_MINUTES = 15;
    
    /**
     * Enregistre un nouvel utilisateur dans le système.
     * <p>
     * Crée un compte utilisateur avec le rôle ROLE_USER par défaut et un compte actif.
     * Le mot de passe est hashé avec BCrypt avant d'être stocké en base de données.
     * </p>
     *
     * @param request Les informations de l'utilisateur à enregistrer
     * @return Les informations de l'utilisateur créé
     * @throws UserAlreadyExistsException si l'email existe déjà dans le système
     */
    @Transactional
    public UserResponse register(RegisterRequest request) {
        Timer.Sample timer = customMetrics.startRegistrationTimer();
        try {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new UserAlreadyExistsException(request.getEmail());
            }
            
            User user = User.builder()
                    .email(request.getEmail())
                    .firstName(request.getFirstName())
                    .lastName(request.getLastName())
                    .password(passwordEncoder.encode(request.getPassword()))
                    .role(Role.ROLE_USER)
                    .active(true)
                    .build();
            
            user = userRepository.save(user);
            log.info("User registered successfully: userId={} email={}", 
                user.getId(), LoggingUtils.maskEmail(user.getEmail()));
            
            customMetrics.incrementUserRegistration();
            return mapToResponse(user);
        } finally {
            customMetrics.recordRegistrationDuration(timer);
        }
    }
    
    /**
     * Authentifie un utilisateur et génère un token JWT.
     * <p>
     * Valide les identifiants (email et mot de passe) et génère un token JWT contenant
     * l'ID utilisateur, l'email et le rôle. Le token a une durée de validité configurable
     * (par défaut 24 heures).
     * </p>
     *
     * @param request Les identifiants de connexion (email et mot de passe)
     * @return Les informations de connexion incluant le token JWT et les informations de l'utilisateur
     * @throws BadCredentialsException si l'email ou le mot de passe est incorrect
     * @throws UserInactiveException si le compte utilisateur est désactivé
     */
    @Transactional
    public LoginResponse login(LoginRequest request) {
        Timer.Sample timer = customMetrics.startLoginTimer();
        try {
            User user = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> {
                        customMetrics.incrementAuthenticationFailure();
                        return new BadCredentialsException();
                    });
            
            checkAccountLocked(user);
            
            if (!user.getActive()) {
                customMetrics.incrementAuthenticationFailure();
                incrementFailedLoginAttempts(user);
                throw new UserInactiveException();
            }
            
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                customMetrics.incrementAuthenticationFailure();
                incrementFailedLoginAttempts(user);
                throw new BadCredentialsException();
            }
            
            resetFailedLoginAttempts(user);
            
            UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
            String token = jwtService.generateToken(userDetails, user.getId());
            
            log.info("[AUDIT] User logged in successfully: userId={} email={}", 
                user.getId(), LoggingUtils.maskEmail(user.getEmail()));
            
            customMetrics.incrementUserLogin();
            
            return LoginResponse.builder()
                    .token(token)
                    .type("Bearer")
                    .userId(user.getId())
                    .email(user.getEmail())
                    .firstName(user.getFirstName())
                    .lastName(user.getLastName())
                    .role(user.getRole().name())
                    .build();
        } finally {
            customMetrics.recordLoginDuration(timer);
        }
    }
    
    /**
     * Vérifie si un compte est verrouillé.
     *
     * @param user L'utilisateur à vérifier
     * @throws AccountLockedException si le compte est verrouillé
     */
    private void checkAccountLocked(User user) {
        if (user.getLockedUntil() != null && user.getLockedUntil().isAfter(LocalDateTime.now())) {
            long minutesRemaining = ChronoUnit.MINUTES.between(LocalDateTime.now(), user.getLockedUntil());
            throw new AccountLockedException(
                String.format("Votre compte est temporairement verrouillé suite à plusieurs tentatives de connexion échouées. Réessayez dans %d minute(s).", 
                    minutesRemaining + 1),
                user.getId(),
                user.getLockedUntil()
            );
        } else if (user.getLockedUntil() != null && user.getLockedUntil().isBefore(LocalDateTime.now())) {
            user.setLockedUntil(null);
            user.setFailedLoginAttempts(0);
            userRepository.save(user);
        }
    }
    
    /**
     * Incrémente le nombre de tentatives de connexion échouées et verrouille le compte si nécessaire.
     */
    private void incrementFailedLoginAttempts(User user) {
        int attempts = (user.getFailedLoginAttempts() != null ? user.getFailedLoginAttempts() : 0) + 1;
        user.setFailedLoginAttempts(attempts);
        
        if (attempts >= MAX_LOGIN_ATTEMPTS) {
            LocalDateTime lockUntil = LocalDateTime.now().plusMinutes(LOCKOUT_DURATION_MINUTES);
            user.setLockedUntil(lockUntil);
            log.warn("[SECURITY] Account locked due to failed login attempts: userId={} email={} lockedUntil={}", 
                user.getId(), LoggingUtils.maskEmail(user.getEmail()), lockUntil);
        }
        
        userRepository.save(user);
    }
    
    /**
     * Réinitialise le nombre de tentatives de connexion échouées après une connexion réussie.
     */
    private void resetFailedLoginAttempts(User user) {
        if (user.getFailedLoginAttempts() != null && user.getFailedLoginAttempts() > 0) {
            user.setFailedLoginAttempts(0);
            user.setLockedUntil(null);
            userRepository.save(user);
        }
    }
    
    private UserResponse mapToResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .role(user.getRole())
                .active(user.getActive())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}

