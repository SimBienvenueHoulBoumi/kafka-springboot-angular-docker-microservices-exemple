package com.simdev.api_users.auth.exception;

/**
 * Exception levée lorsque le compte utilisateur est inactif
 */
public class UserInactiveException extends RuntimeException {
    public UserInactiveException() {
        super("Le compte utilisateur est inactif");
    }
}

