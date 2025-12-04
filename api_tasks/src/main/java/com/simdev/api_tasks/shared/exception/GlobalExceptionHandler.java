package com.simdev.api_tasks.shared.exception;

import com.simdev.api_tasks.task.exception.ResourceNotFoundException;
import com.simdev.api_tasks.task.exception.UnauthorizedAccessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire global des exceptions pour le service Tasks.
 * <p>
 * Ce gestionnaire centralise la gestion de toutes les exceptions et retourne
 * des messages d'erreur clairs et structurés aux clients.
 * </p>
 *
 * @author API Tasks Service
 * @version 1.0
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
        body.put("detail", "La tâche demandée n'existe pas ou a été supprimée. Vérifiez l'identifiant fourni.");
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
    }
    
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, Object>> handleUserNotFoundException(UserNotFoundException ex) {
        log.warn("User not found: {}", ex.getMessage());
        
        Map<String, Object> body = new HashMap<>();
        body.put("timestamp", LocalDateTime.now());
        body.put("status", HttpStatus.NOT_FOUND.value());
        body.put("error", "Utilisateur non trouvé");
        body.put("errorCode", ErrorCode.USER_NOT_FOUND.getCode());
        body.put("message", ex.getMessage());
        body.put("detail", "L'utilisateur spécifié n'existe pas dans le système. Vérifiez l'identifiant fourni.");
        
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(body);
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
        body.put("detail", "Vous n'avez pas les permissions nécessaires pour effectuer cette action. Vous pouvez uniquement modifier ou supprimer vos propres tâches.");
        
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

