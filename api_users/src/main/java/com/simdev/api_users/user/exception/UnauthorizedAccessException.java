package com.simdev.api_users.user.exception;

/**
 * Exception levée lorsqu'un accès non autorisé est tenté
 */
public class UnauthorizedAccessException extends RuntimeException {
    public UnauthorizedAccessException(String message) {
        super(message);
    }
}

