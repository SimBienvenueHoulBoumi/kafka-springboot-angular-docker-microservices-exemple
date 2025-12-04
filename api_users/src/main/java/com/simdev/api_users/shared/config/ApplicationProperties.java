package com.simdev.api_users.shared.config;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "app")
public class ApplicationProperties {
    
    @NotNull(message = "La configuration JWT est requise")
    private JwtProperties jwt;
    
    @NotNull(message = "La configuration Admin est requise")
    private AdminProperties admin;
    
    @NotNull(message = "La configuration Database est requise")
    private DatabaseProperties database;
    
    @Data
    public static class JwtProperties {
        @NotBlank(message = "JWT secret ne peut pas être vide")
        private String secret;
        
        @NotNull(message = "JWT expiration est requise")
        @Positive(message = "JWT expiration doit être positive")
        private Long expiration;
    }
    
    @Data
    public static class AdminProperties {
        @NotBlank(message = "Email admin ne peut pas être vide")
        @Email(message = "Email admin doit être valide")
        private String email;
        
        @NotBlank(message = "Mot de passe admin ne peut pas être vide")
        private String password;
        
        @NotBlank(message = "Prénom admin ne peut pas être vide")
        private String firstName;
        
        @NotBlank(message = "Nom admin ne peut pas être vide")
        private String lastName;
    }
    
    @Data
    public static class DatabaseProperties {
        @NotBlank(message = "URL de la base de données est requise")
        private String url;
        
        @NotBlank(message = "Username de la base de données est requis")
        private String username;
        
        @NotBlank(message = "Password de la base de données est requis")
        private String password;
    }
}

