package com.simdev.api_users.user.service;

import com.simdev.api_users.user.domain.Role;
import com.simdev.api_users.user.domain.User;
import com.simdev.api_users.user.dto.UserRequest;
import com.simdev.api_users.user.dto.UserResponse;
import com.simdev.api_users.user.exception.ResourceNotFoundException;
import com.simdev.api_users.user.exception.UserAlreadyExistsException;
import com.simdev.api_users.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.simdev.api_users.shared.metrics.CustomMetrics;
import com.simdev.api_users.shared.service.OutboxService;
import com.simdev.api_users.shared.util.LoggingUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service de gestion des utilisateurs (opérations CRUD).
 * <p>
 * Ce service gère toutes les opérations sur les utilisateurs :
 * - Création, lecture, mise à jour et suppression d'utilisateurs
 * - Validation de l'existence d'utilisateurs
 * - Publication d'événements Kafka via le pattern Transactional Outbox
 * <p>
 * <strong>Pattern Transactional Outbox :</strong>
 * Toutes les modifications d'utilisateurs (création, mise à jour, suppression)
 * déclenchent la création d'un événement dans la table outbox dans la même transaction.
 * Le service {@link OutboxService} publie ensuite ces événements sur Kafka de manière asynchrone,
 * garantissant ainsi la cohérence transactionnelle et la livraison garantie des événements.
 * </p>
 * <p>
 * <strong>Événements Kafka publiés :</strong>
 * <ul>
 *   <li>{@code user.created} - Lors de la création d'un utilisateur</li>
 *   <li>{@code user.updated} - Lors de la mise à jour d'un utilisateur</li>
 *   <li>{@code user.deleted} - Lors de la suppression d'un utilisateur</li>
 * </ul>
 * Ces événements sont consommés par les autres microservices (comme le service Tasks)
 * pour maintenir la synchronisation des données.
 * </p>
 * <p>
 * <strong>Sécurité :</strong>
 * Les opérations d'administration (création, modification, suppression de tous les utilisateurs)
 * doivent être protégées par les contrôleurs qui utilisent ce service.
 * L'endpoint {@code /users/{id}/exists} est public pour permettre la validation
 * inter-services.
 * </p>
 *
 * @author API Users Service
 * @version 1.0
 * @see OutboxService
 * @see UserRepository
 * @see CustomMetrics
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserService {
    
    private final UserRepository userRepository;
    private final OutboxService outboxService;
    private final PasswordEncoder passwordEncoder;
    private final CustomMetrics customMetrics;
    private final ObjectMapper objectMapper;
    private static final String USER_TOPIC = "user-events";
    
    @Transactional
    public UserResponse createUser(UserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException(request.getEmail());
        }
        
        User user = User.builder()
                .email(request.getEmail())
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .password(passwordEncoder.encode(request.getPassword()))
                .role(Role.ROLE_USER)
                .active(request.getActive() != null ? request.getActive() : true)
                .build();
        
        user = userRepository.save(user);
        
        // Publier événement via Outbox Pattern (garantit la livraison)
        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("eventType", "CREATED");
        eventPayload.put("userId", user.getId());
        eventPayload.put("email", user.getEmail());
        eventPayload.put("timestamp", LocalDateTime.now().toString());
        
        try {
            String payload = objectMapper.writeValueAsString(eventPayload);
            outboxService.createEvent("user.created", USER_TOPIC, payload, user.getId().toString());
        } catch (Exception e) {
            log.error("Failed to create outbox event for user creation: userId={}", user.getId(), e);
            // Ne pas faire échouer la transaction si l'outbox échoue
        }
        
        log.info("User created successfully: userId={} email={}", 
            user.getId(), LoggingUtils.maskEmail(user.getEmail()));
        customMetrics.incrementUserCreated();
        return mapToResponse(user);
    }
    
    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));
        return mapToResponse(user);
    }
    
    @Transactional(readOnly = true)
    public boolean userExists(Long id) {
        return userRepository.existsById(id);
    }
    
    @Transactional
    public UserResponse updateUser(Long id, UserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));
        
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        if (request.getPassword() != null && !request.getPassword().isEmpty()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        if (request.getActive() != null) {
            user.setActive(request.getActive());
        }
        
        user = userRepository.save(user);
        
        // Publier événement via Outbox Pattern
        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("eventType", "UPDATED");
        eventPayload.put("userId", user.getId());
        eventPayload.put("email", user.getEmail());
        eventPayload.put("timestamp", LocalDateTime.now().toString());
        
        try {
            String payload = objectMapper.writeValueAsString(eventPayload);
            outboxService.createEvent("user.updated", USER_TOPIC, payload, user.getId().toString());
        } catch (Exception e) {
            log.error("Failed to create outbox event for user update: userId={}", user.getId(), e);
        }
        
        log.info("User updated successfully: userId={} email={}", 
            user.getId(), LoggingUtils.maskEmail(user.getEmail()));
        customMetrics.incrementUserUpdated();
        return mapToResponse(user);
    }
    
    @Transactional
    public void deleteUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", id));
        
        Long userId = user.getId();
        String userEmail = user.getEmail();
        
        userRepository.delete(user);
        
        // Publier événement via Outbox Pattern
        Map<String, Object> eventPayload = new HashMap<>();
        eventPayload.put("eventType", "DELETED");
        eventPayload.put("userId", userId);
        eventPayload.put("email", userEmail);
        eventPayload.put("timestamp", LocalDateTime.now().toString());
        
        try {
            String payload = objectMapper.writeValueAsString(eventPayload);
            outboxService.createEvent("user.deleted", USER_TOPIC, payload, userId.toString());
        } catch (Exception e) {
            log.error("Failed to create outbox event for user deletion: userId={}", userId, e);
        }
        
        log.info("User deleted successfully: userId={} email={}", 
            userId, LoggingUtils.maskEmail(userEmail));
        customMetrics.incrementUserDeleted();
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

