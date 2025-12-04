package com.simdev.api_tasks.task.exception;

/**
 * Exception levée lorsqu'une ressource n'est pas trouvée
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " non trouvé avec l'ID : " + id);
    }
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

