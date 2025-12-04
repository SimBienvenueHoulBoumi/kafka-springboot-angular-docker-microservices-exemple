package com.simdev.api_users.shared.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * Service pour les métriques personnalisées
 * Permet de suivre les performances et les événements métier
 */
@Component
@RequiredArgsConstructor
public class CustomMetrics {
    
    private final MeterRegistry meterRegistry;
    
    private Counter userRegistrationCounter;
    private Counter userLoginCounter;
    private Counter userCreatedCounter;
    private Counter userUpdatedCounter;
    private Counter userDeletedCounter;
    private Counter authenticationFailureCounter;
    
    private Timer loginDurationTimer;
    private Timer registrationDurationTimer;
    
    @PostConstruct
    public void init() {
        userRegistrationCounter = Counter.builder("user.registration.total")
                .description("Total number of user registrations")
                .register(meterRegistry);
        
        userLoginCounter = Counter.builder("user.login.total")
                .description("Total number of user logins")
                .register(meterRegistry);
        
        userCreatedCounter = Counter.builder("user.created.total")
                .description("Total number of users created")
                .register(meterRegistry);
        
        userUpdatedCounter = Counter.builder("user.updated.total")
                .description("Total number of users updated")
                .register(meterRegistry);
        
        userDeletedCounter = Counter.builder("user.deleted.total")
                .description("Total number of users deleted")
                .register(meterRegistry);
        
        authenticationFailureCounter = Counter.builder("authentication.failure.total")
                .description("Total number of authentication failures")
                .tag("type", "invalid_credentials")
                .register(meterRegistry);
        
        loginDurationTimer = Timer.builder("user.login.duration")
                .description("Time taken for login operations")
                .register(meterRegistry);
        
        registrationDurationTimer = Timer.builder("user.registration.duration")
                .description("Time taken for registration operations")
                .register(meterRegistry);
    }
    
    public void incrementUserRegistration() {
        userRegistrationCounter.increment();
    }
    
    public void incrementUserLogin() {
        userLoginCounter.increment();
    }
    
    public void incrementUserCreated() {
        userCreatedCounter.increment();
    }
    
    public void incrementUserUpdated() {
        userUpdatedCounter.increment();
    }
    
    public void incrementUserDeleted() {
        userDeletedCounter.increment();
    }
    
    public void incrementAuthenticationFailure() {
        authenticationFailureCounter.increment();
    }
    
    public Timer.Sample startLoginTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordLoginDuration(Timer.Sample sample) {
        sample.stop(loginDurationTimer);
    }
    
    public Timer.Sample startRegistrationTimer() {
        return Timer.start(meterRegistry);
    }
    
    public void recordRegistrationDuration(Timer.Sample sample) {
        sample.stop(registrationDurationTimer);
    }
}

