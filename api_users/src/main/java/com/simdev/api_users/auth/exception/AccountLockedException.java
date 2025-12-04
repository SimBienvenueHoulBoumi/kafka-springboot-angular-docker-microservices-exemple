package com.simdev.api_users.auth.exception;

import java.time.LocalDateTime;

/**
 * Exception levée lorsqu'un compte utilisateur est verrouillé
 * suite à plusieurs tentatives de connexion échouées.
 */
public class AccountLockedException extends RuntimeException {
    
    private final Long userId;
    private final LocalDateTime lockedUntil;
    
    public AccountLockedException(String message, Long userId, LocalDateTime lockedUntil) {
        super(message);
        this.userId = userId;
        this.lockedUntil = lockedUntil;
    }
    
    public AccountLockedException(String message) {
        super(message);
        this.userId = null;
        this.lockedUntil = null;
    }
    
    public Long getUserId() {
        return userId;
    }
    
    public LocalDateTime getLockedUntil() {
        return lockedUntil;
    }
}

