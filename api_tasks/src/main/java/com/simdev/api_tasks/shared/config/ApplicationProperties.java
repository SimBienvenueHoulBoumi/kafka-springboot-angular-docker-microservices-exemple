package com.simdev.api_tasks.shared.config;

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
    
    @NotNull(message = "La configuration Database est requise")
    private DatabaseProperties database;
    
    @NotNull(message = "La configuration Services est requise")
    private ServicesProperties services;
    
    @NotNull(message = "La configuration Cache est requise")
    private CacheProperties cache;
    
    @Data
    public static class DatabaseProperties {
        @NotBlank(message = "URL de la base de données est requise")
        private String url;
        
        @NotBlank(message = "Username de la base de données est requis")
        private String username;
        
        @NotBlank(message = "Password de la base de données est requis")
        private String password;
    }
    
    @Data
    public static class ServicesProperties {
        @NotNull(message = "La configuration Users Service est requise")
        private UsersServiceProperties users;
    }
    
    @Data
    public static class UsersServiceProperties {
        @NotBlank(message = "URL du service Users est requise")
        private String url;
        
        @NotNull(message = "Timeout du service Users est requis")
        @Positive(message = "Timeout doit être positif")
        private Long timeout;
    }
    
    @Data
    public static class CacheProperties {
        @NotNull(message = "La configuration User Cache est requise")
        private UserCacheProperties user;
    }
    
    @Data
    public static class UserCacheProperties {
        @NotNull(message = "TTL du cache utilisateur est requis")
        @Positive(message = "TTL doit être positif")
        private Long ttlSeconds;
    }
}

