package com.simdev.api_tasks.shared.exception;

/**
 * Exception levée lorsqu'un utilisateur n'est pas trouvé.
 * <p>
 * Cette exception est utilisée lorsque le service Tasks tente de valider
 * l'existence d'un utilisateur mais celui-ci n'existe pas dans le service Users.
 * </p>
 *
 * @author API Tasks Service
 * @version 1.0
 */
public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException(Long userId) {
        super("Utilisateur non trouvé avec l'ID : " + userId);
    }
    
    public UserNotFoundException(String message) {
        super(message);
    }
}

