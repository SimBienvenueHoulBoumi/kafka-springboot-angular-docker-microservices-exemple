package com.simdev.api_users.shared.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class ConfigurationValidator implements CommandLineRunner {
    
    private final Environment environment;
    private final ApplicationProperties applicationProperties;
    
    @Override
    public void run(String... args) {
        log.info("Starting configuration validation...");
        
        validateJwtConfiguration();
        validateDatabaseConfiguration();
        validateKafkaConfiguration();
        
        log.info("✅ Configuration validation completed successfully");
    }
    
    private void validateJwtConfiguration() {
        String jwtSecret = environment.getProperty("jwt.secret");
        if (jwtSecret == null || jwtSecret.length() < 32) {
            log.warn("⚠️ JWT secret est trop court (minimum 32 caractères). Utilisez une clé plus forte en production.");
        }
        
        log.info("JWT configuration validated: secret_length={}, expiration={}ms", 
            jwtSecret != null ? jwtSecret.length() : 0,
            applicationProperties.getJwt().getExpiration());
    }
    
    private void validateDatabaseConfiguration() {
        String dbUrl = environment.getProperty("spring.datasource.url");
        if (dbUrl != null && dbUrl.contains("localhost")) {
            log.info("Database configuration: Using local development database");
        }
        log.debug("Database URL configured: {}", dbUrl != null ? maskUrl(dbUrl) : "not set");
    }
    
    private void validateKafkaConfiguration() {
        String kafkaServers = environment.getProperty("spring.kafka.bootstrap-servers");
        if (kafkaServers == null || kafkaServers.isEmpty()) {
            log.error("❌ Kafka bootstrap servers not configured!");
            throw new IllegalStateException("Kafka bootstrap servers must be configured");
        }
        log.info("Kafka configuration validated: bootstrap_servers={}", kafkaServers);
    }
    
    private String maskUrl(String url) {
        return url.replaceAll("://([^:]+):([^@]+)@", "://***:***@");
    }
}

