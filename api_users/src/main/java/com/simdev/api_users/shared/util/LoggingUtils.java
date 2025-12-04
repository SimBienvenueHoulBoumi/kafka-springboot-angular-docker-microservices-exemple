package com.simdev.api_users.shared.util;

/**
 * Utilitaires pour le logging sécurisé
 * Masque les données sensibles dans les logs
 */
public class LoggingUtils {
    
    /**
     * Masque un email dans les logs pour protéger la vie privée
     * Exemple: "john.doe@example.com" -> "jo***@example.com"
     */
    public static String maskEmail(String email) {
        if (email == null || email.isEmpty()) {
            return "N/A";
        }
        
        int atIndex = email.indexOf('@');
        if (atIndex <= 0) {
            return "***";
        }
        
        String localPart = email.substring(0, Math.min(2, atIndex));
        String domain = email.substring(atIndex + 1);
        
        return localPart + "***@" + domain;
    }
    
    /**
     * Masque partiellement un ID pour le logging
     * Exemple: 12345 -> "***2345"
     */
    public static String maskId(Long id) {
        if (id == null) {
            return "N/A";
        }
        
        String idStr = String.valueOf(id);
        if (idStr.length() <= 4) {
            return "***";
        }
        
        return "***" + idStr.substring(idStr.length() - 4);
    }
    
    /**
     * Masque un token JWT (ne montre que les premiers et derniers caractères)
     * Exemple: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..." -> "eyJ***...xyz"
     */
    public static String maskToken(String token) {
        if (token == null || token.isEmpty()) {
            return "N/A";
        }
        
        if (token.length() <= 10) {
            return "***";
        }
        
        return token.substring(0, 3) + "***" + token.substring(token.length() - 3);
    }
}

