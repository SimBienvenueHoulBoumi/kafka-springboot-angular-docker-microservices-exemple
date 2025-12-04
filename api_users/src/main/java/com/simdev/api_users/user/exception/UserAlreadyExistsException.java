package com.simdev.api_users.user.exception;

/**
 * Exception levée lorsqu'un utilisateur existe déjà (email déjà utilisé)
 */
public class UserAlreadyExistsException extends RuntimeException {
    public UserAlreadyExistsException(String email) {
        super("Un utilisateur avec l'email '" + email + "' existe déjà");
    }
}

