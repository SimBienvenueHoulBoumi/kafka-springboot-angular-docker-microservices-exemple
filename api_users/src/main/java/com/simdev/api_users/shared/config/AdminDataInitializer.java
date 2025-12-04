package com.simdev.api_users.shared.config;

import com.simdev.api_users.user.domain.Role;
import com.simdev.api_users.user.domain.User;
import com.simdev.api_users.user.repository.UserRepository;
import com.simdev.api_users.shared.util.LoggingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Initialiseur de données pour créer l'utilisateur administrateur au démarrage.
 * <p>
 * Cette classe implémente {@link CommandLineRunner} et s'exécute automatiquement
 * au démarrage de l'application Spring Boot. Elle vérifie si un utilisateur admin
 * existe et en crée un si nécessaire.
 * </p>
 * <p>
 * <strong>Fonctionnement :</strong>
 * <ol>
 *   <li>Vérifie si un utilisateur avec l'email admin existe déjà</li>
 *   <li>Si non, crée un utilisateur avec le rôle {@link Role#ROLE_ADMIN}</li>
 *   <li>Le mot de passe est hashé avec BCrypt avant stockage</li>
 *   <li>L'utilisateur est créé avec le statut actif ({@code active = true})</li>
 * </ol>
 * </p>
 * <p>
 * <strong>Configuration requise :</strong>
 * Les informations de l'admin doivent être configurées dans {@code application.yml} :
 * <pre>
 * admin:
 *   email: admin@example.com
 *   password: AdminPassword123!
 *   first-name: Admin
 *   last-name: User
 * </pre>
 * </p>
 * <p>
 * <strong>Sécurité :</strong>
 * ⚠️ <strong>Important :</strong> En production, changez immédiatement le mot de passe
 * admin après le premier démarrage ! Le mot de passe par défaut ne doit jamais être
 * utilisé en production.
 * </p>
 * <p>
 * <strong>Idempotence :</strong>
 * Si l'utilisateur admin existe déjà, aucune action n'est effectuée. Cette classe
 * peut donc être exécutée plusieurs fois sans créer de doublons.
 * </p>
 *
 * @author API Users Service
 * @version 1.0
 * @see CommandLineRunner
 * @see UserRepository
 * @see PasswordEncoder
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AdminDataInitializer implements CommandLineRunner {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Value("${admin.email}")
    private String adminEmail;
    
    @Value("${admin.password}")
    private String adminPassword;
    
    @Value("${admin.first-name}")
    private String adminFirstName;
    
    @Value("${admin.last-name}")
    private String adminLastName;
    
    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail(adminEmail)) {
            User admin = User.builder()
                    .email(adminEmail)
                    .firstName(adminFirstName)
                    .lastName(adminLastName)
                    .password(passwordEncoder.encode(adminPassword))
                    .role(Role.ROLE_ADMIN)
                    .active(true)
                    .build();
            
            admin = userRepository.save(admin);
            log.info("Admin user created successfully: userId={} email={} role=ROLE_ADMIN", 
                admin.getId(), LoggingUtils.maskEmail(adminEmail));
        } else {
            log.info("Admin user already exists: email={}", LoggingUtils.maskEmail(adminEmail));
        }
    }
}

