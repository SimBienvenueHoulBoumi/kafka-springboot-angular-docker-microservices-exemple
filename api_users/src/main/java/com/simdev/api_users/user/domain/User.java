package com.simdev.api_users.user.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entité JPA représentant un utilisateur du système.
 * <p>
 * Cette entité représente les utilisateurs de l'application avec leurs informations
 * personnelles, leurs identifiants d'authentification, leur rôle et leur statut.
 * </p>
 * <p>
 * <strong>Informations stockées :</strong>
 * <ul>
 *   <li><strong>Identifiants :</strong> ID unique, email (unique), prénom, nom</li>
 *   <li><strong>Authentification :</strong> Mot de passe hashé (BCrypt), rôle ({@link Role})</li>
 *   <li><strong>Statut :</strong> Actif/inactif, tentatives de connexion échouées, date de verrouillage</li>
 *   <li><strong>Audit :</strong> Date de création et de dernière mise à jour</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Sécurité :</strong>
 * <ul>
 *   <li>Le mot de passe est stocké hashé avec BCrypt (via {@link PasswordEncoder})</li>
 *   <li>L'email est unique et utilisé comme identifiant de connexion</li>
 *   <li>Le rôle détermine les permissions (ROLE_USER ou ROLE_ADMIN)</li>
 *   <li>Le compte peut être désactivé via le champ {@code active}</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Système de verrouillage de compte :</strong>
 * Pour protéger contre les attaques par force brute :
 * <ul>
 *   <li>{@code failedLoginAttempts} : Compte le nombre de tentatives échouées</li>
 *   <li>{@code lockedUntil} : Date/heure jusqu'à laquelle le compte est verrouillé
 *       (null si non verrouillé)</li>
 *   <li>Le verrouillage est géré automatiquement par {@link AuthService}</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Callbacks JPA :</strong>
 * <ul>
 *   <li>{@code @PrePersist} : Initialise {@code createdAt} et {@code updatedAt} à la création</li>
 *   <li>{@code @PreUpdate} : Met à jour {@code updatedAt} à chaque modification</li>
 * </ul>
 * </p>
 *
 * @author API Users Service
 * @version 1.0
 * @see Role
 * @see UserRepository
 * @see AuthService
 */
@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false, unique = true)
    private String email;
    
    @Column(nullable = false)
    private String firstName;
    
    @Column(nullable = false)
    private String lastName;
    
    @Column(nullable = false)
    private String password;
    
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Role role = Role.ROLE_USER;
    
    @Column(nullable = false)
    @Builder.Default
    private Boolean active = true;
    
    @Column(nullable = false)
    @Builder.Default
    private Integer failedLoginAttempts = 0;
    
    @Column
    private LocalDateTime lockedUntil;
    
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @Column(nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}

