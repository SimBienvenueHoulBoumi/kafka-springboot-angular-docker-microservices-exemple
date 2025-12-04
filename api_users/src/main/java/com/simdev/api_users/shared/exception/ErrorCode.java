package com.simdev.api_users.shared.exception;

/**
 * Codes d'erreur standardisés pour l'API
 */
public enum ErrorCode {
    BAD_CREDENTIALS(1001, "Identifiants invalides"),
    USER_INACTIVE(1002, "Compte utilisateur inactif"),
    ACCOUNT_LOCKED(1003, "Compte verrouillé"),
    TOKEN_INVALID(1004, "Token JWT invalide"),
    TOKEN_EXPIRED(1005, "Token JWT expiré"),
    
    RESOURCE_NOT_FOUND(2001, "Ressource non trouvée"),
    USER_NOT_FOUND(2002, "Utilisateur non trouvé"),
    USER_ALREADY_EXISTS(2003, "Utilisateur déjà existant"),
    
    VALIDATION_ERROR(3001, "Erreur de validation"),
    INVALID_INPUT(3002, "Données d'entrée invalides"),
    
    CONFIGURATION_ERROR(4001, "Erreur de configuration"),
    
    INTERNAL_ERROR(5001, "Erreur interne du serveur"),
    
    RATE_LIMIT_EXCEEDED(6001, "Limite de requêtes dépassée");
    
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

