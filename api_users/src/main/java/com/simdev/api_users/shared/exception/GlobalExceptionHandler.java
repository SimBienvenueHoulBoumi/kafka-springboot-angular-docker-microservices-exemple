package com.simdev.api_users.shared.exception;

import com.simdev.api_users.auth.exception.AccountLockedException;
import com.simdev.api_users.auth.exception.BadCredentialsException;
import com.simdev.api_users.auth.exception.UserInactiveException;
import com.simdev.api_users.user.exception.ResourceNotFoundException;
import com.simdev.api_users.user.exception.UnauthorizedAccessException;
import com.simdev.api_users.user.exception.UserAlreadyExistsException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire global des exceptions pour l'API Users.
 * <p>
 * Ce gestionnaire intercepte toutes les exceptions levées par les contrôleurs et services
 * et les transforme en réponses HTTP structurées et cohérentes. Il assure une gestion
 * centralisée des erreurs avec des messages formatés et des codes d'erreur standardisés.
 * </p>
 * <p>
 * <strong>Fonctionnalités :</strong>
 * <ul>
 *   <li><strong>Gestion centralisée :</strong> Toutes les exceptions sont traitées au même endroit</li>
 *   <li><strong>Réponses standardisées :</strong> Format JSON cohérent pour toutes les erreurs</li>
 *   <li><strong>Codes d'erreur :</strong> Utilisation de {@link ErrorCode} pour identifier le type d'erreur</li>
 *   <li><strong>Logging :</strong> Enregistrement de toutes les exceptions pour le monitoring</li>
 *   <li><strong>Messages utilisateur :</strong> Messages clairs et actionnables pour les clients</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Exceptions gérées :</strong>
 * <ul>
 *   <li>{@link ResourceNotFoundException} - 404 NOT_FOUND</li>
 *   <li>{@link UserAlreadyExistsException} - 409 CONFLICT</li>
 *   <li>{@link BadCredentialsException} - 401 UNAUTHORIZED</li>
 *   <li>{@link AccountLockedException} - 423 LOCKED</li>
 *   <li>{@link UserInactiveException} - 403 FORBIDDEN</li>
 *   <li>{@link UnauthorizedAccessException} - 403 FORBIDDEN</li>
 *   <li>{@link MethodArgumentNotValidException} - 400 BAD_REQUEST (validation)</li>
 *   <li>{@link Exception} - 500 INTERNAL_SERVER_ERROR (catch-all)</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Format de réponse d'erreur :</strong>
 * <pre>
 * {
 *   "timestamp": "2025-12-04T12:00:00",
 *   "status": 404,
 *   "error": "Ressource non trouvée",
 *   "errorCode": 1001,
 *   "message": "Utilisateur avec l'ID 123 non trouvé",
 *   "detail": "La ressource demandée n'existe pas..."
 * }
 * </pre>
 * </p>
 *
 * @author API Users Service
 * @version 1.0
 * @see ErrorCode
 * @see RestControllerAdvice
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleResourceNotFoundException(ResourceNotFoundException ex) {
        log.warn("Resource not found: {}", ex.getMessage());
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Ressource non trouvée");
        body.put("errorCode", ErrorCode.RESOURCE_NOT_FOUND.getCode());
        body.put("message", ex.getMessage());
        body.put("detail", "La ressource demandée n'existe pas ou a été supprimée. Vérifiez l'identifiant fourni.");
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }
    
    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<Map<String, Object>> handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        log.warn("User already exists: {}", ex.getMessage());
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.CONFLICT.value());
        body.put("error", "Utilisateur déjà existant");
        body.put("errorCode", ErrorCode.USER_ALREADY_EXISTS.getCode());
        body.put("message", ex.getMessage());
        body.put("detail", "Un compte existe déjà avec cette adresse email. Utilisez une autre adresse email ou connectez-vous à votre compte existant.");
        
        return ResponseEntity.status(HttpStatus.CONFLICT).body(body);
    }
    
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<Map<String, Object>> handleBadCredentialsException(BadCredentialsException ex) {
        log.warn("Bad credentials: {}", ex.getMessage() != null ? ex.getMessage() : "Invalid credentials");
        
        String detailMessage = "L'adresse email ou le mot de passe est incorrect. Vérifiez vos identifiants et réessayez.";
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.UNAUTHORIZED.value());
        body.put("error", "Identifiants invalides");
        body.put("errorCode", ErrorCode.BAD_CREDENTIALS.getCode());
        body.put("message", ex.getMessage() != null && !ex.getMessage().isEmpty() 
            ? ex.getMessage() 
            : "Email ou mot de passe incorrect");
        body.put("detail", detailMessage);
        
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(body);
    }
    
    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<Map<String, Object>> handleAccountLockedException(AccountLockedException ex) {
        log.warn("Account locked: {}", ex.getMessage());

        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", 423);
        body.put("error", "Compte verrouillé");
        body.put("errorCode", ErrorCode.ACCOUNT_LOCKED.getCode());
        body.put("message", ex.getMessage());
        if (ex.getLockedUntil() != null) {
            body.put("lockedUntil", ex.getLockedUntil());
            long minutesRemaining = ChronoUnit.MINUTES.between(
                LocalDateTime.now(), ex.getLockedUntil());
            body.put("detail", String.format(
                "Votre compte est temporairement verrouillé. Réessayez dans %d minute(s). " +
                "Le verrouillage sera levé automatiquement après ce délai.", 
                Math.max(1, minutesRemaining + 1)));
        } else {
            body.put("detail", "Votre compte est temporairement verrouillé suite à plusieurs tentatives de connexion échouées. Réessayez plus tard.");
        }

        return ResponseEntity.status(423).body(body);
    }
    
    @ExceptionHandler(UserInactiveException.class)
    public ResponseEntity<Map<String, Object>> handleUserInactiveException(UserInactiveException ex) {
        log.warn("User inactive: {}", ex.getMessage());
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", "Compte inactif");
        body.put("errorCode", ErrorCode.USER_INACTIVE.getCode());
        body.put("message", ex.getMessage());
        body.put("detail", "Votre compte a été désactivé. Contactez l'administrateur pour réactiver votre compte.");
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }
    
    @ExceptionHandler(UnauthorizedAccessException.class)
    public ResponseEntity<Map<String, Object>> handleUnauthorizedAccessException(UnauthorizedAccessException ex) {
        log.warn("Unauthorized access: {}", ex.getMessage());
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.FORBIDDEN.value());
        body.put("error", "Accès non autorisé");
        body.put("errorCode", 4031);
        body.put("message", ex.getMessage());
        body.put("detail", "Vous n'avez pas les permissions nécessaires pour effectuer cette action. Vous pouvez uniquement modifier ou supprimer votre propre profil.");
        
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(body);
    }
    
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationException(MethodArgumentNotValidException ex) {
        log.warn("Validation error: {}", ex.getMessage());
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage != null ? errorMessage : "Erreur de validation");
        });
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Erreur de validation");
        body.put("errorCode", ErrorCode.VALIDATION_ERROR.getCode());
        body.put("message", "Les données fournies ne respectent pas les règles de validation");
        body.put("detail", "Veuillez corriger les erreurs ci-dessous et réessayer.");
        body.put("errors", errors);
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
    
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        log.warn("Illegal argument: {}", ex.getMessage());
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.BAD_REQUEST.value());
        body.put("error", "Requête invalide");
        body.put("errorCode", ErrorCode.INVALID_INPUT.getCode());
        String userMessage = ex.getMessage() != null && !ex.getMessage().contains("java") 
            ? ex.getMessage() 
            : "Les paramètres de la requête sont invalides ou mal formatés";
        body.put("message", userMessage);
        body.put("detail", "Vérifiez le format et les valeurs des paramètres de votre requête.");
        
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }
    
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalStateException(IllegalStateException ex) {
        log.error("Illegal state: {}", ex.getMessage());
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Erreur de configuration");
        body.put("errorCode", ErrorCode.CONFIGURATION_ERROR.getCode());
        body.put("message", "Le service rencontre un problème de configuration");
        body.put("detail", "Veuillez réessayer dans quelques instants. Si le problème persiste, contactez le support technique.");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
    
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("Unexpected error occurred", ex);
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put("error", "Erreur serveur");
        body.put("errorCode", ErrorCode.INTERNAL_ERROR.getCode());
        body.put("message", "Une erreur inattendue s'est produite lors du traitement de votre requête");
        body.put("detail", "Veuillez réessayer dans quelques instants. Si le problème persiste, contactez le support technique avec le code d'erreur ci-dessus.");
        
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

