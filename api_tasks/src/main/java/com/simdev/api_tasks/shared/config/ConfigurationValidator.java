package com.simdev.api_tasks.shared.config;

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
    
    @Override
    public void run(String... args) {
        log.info("Starting configuration validation...");
        
        validateDatabaseConfiguration();
        validateServicesConfiguration();
        validateKafkaConfiguration();
        validateResilience4jConfiguration();
        
        log.info("✅ Configuration validation completed successfully");
    }
    
    private void validateDatabaseConfiguration() {
        String dbUrl = environment.getProperty("spring.datasource.url");
        if (dbUrl != null && dbUrl.contains("localhost")) {
            log.info("Database configuration: Using local development database");
        }
        log.debug("Database URL configured: {}", dbUrl != null ? maskUrl(dbUrl) : "not set");
    }
    
    private void validateServicesConfiguration() {
        String usersServiceUrl = environment.getProperty("services.users.url");
        if (usersServiceUrl == null || usersServiceUrl.isEmpty()) {
            log.warn("⚠️ Users service URL not configured. Default will be used.");
        } else {
            log.info("Services configuration validated: users_service_url={}", usersServiceUrl);
        }
    }
    
    private void validateKafkaConfiguration() {
        String kafkaServers = environment.getProperty("spring.kafka.bootstrap-servers");
        if (kafkaServers == null || kafkaServers.isEmpty()) {
            log.error("❌ Kafka bootstrap servers not configured!");
            throw new IllegalStateException("Kafka bootstrap servers must be configured");
        }
        log.info("Kafka configuration validated: bootstrap_servers={}", kafkaServers);
    }
    
    private void validateResilience4jConfiguration() {
        String circuitBreakerConfig = environment.getProperty("resilience4j.circuitbreaker.instances.userService.failureRateThreshold");
        if (circuitBreakerConfig != null) {
            log.info("Resilience4j configuration validated: circuit_breaker_threshold={}%", circuitBreakerConfig);
        }
    }
    
    private String maskUrl(String url) {
        return url.replaceAll("://([^:]+):([^@]+)@", "://***:***@");
    }
}

