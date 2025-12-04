package com.simdev.api_tasks.shared.exception;

/**
 * Codes d'erreur standardisés pour l'API Tasks.
 * <p>
 * Ces codes d'erreur sont utilisés dans les réponses d'erreur pour permettre
 * un traitement uniforme côté client.
 * </p>
 *
 * @author API Tasks Service
 * @version 1.0
 */
public enum ErrorCode {
    RESOURCE_NOT_FOUND(2001, "Ressource non trouvée"),
    TASK_NOT_FOUND(2002, "Tâche non trouvée"),
    USER_NOT_FOUND(2003, "Utilisateur non trouvé"),
    
    VALIDATION_ERROR(3001, "Erreur de validation"),
    INVALID_INPUT(3002, "Données d'entrée invalides"),
    
    INTERNAL_ERROR(5001, "Erreur interne du serveur");
    
    private final int code;
    private final String message;
    
    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
    
    public int getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
}

