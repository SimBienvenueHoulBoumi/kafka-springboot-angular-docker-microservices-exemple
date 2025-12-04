package com.simdev.api_users.user.exception;

/**
 * Exception levée lorsqu'une ressource demandée n'est pas trouvée dans le système.
 * <p>
 * Cette exception est utilisée lorsqu'une opération tente d'accéder à une ressource
 * (utilisateur, tâche, etc.) qui n'existe pas ou qui a été supprimée.
 * </p>
 * <p>
 * <strong>Gestion :</strong>
 * Cette exception est interceptée par {@link com.simdev.api_users.shared.exception.GlobalExceptionHandler}
 * et transformée en réponse HTTP 404 (NOT_FOUND) avec un message formaté.
 * </p>
 * <p>
 * <strong>Exemples d'utilisation :</strong>
 * <ul>
 *   <li>Recherche d'un utilisateur par ID inexistant</li>
 *   <li>Accès à une ressource supprimée</li>
 *   <li>Opération sur un ID invalide</li>
 * </ul>
 * </p>
 *
 * @author API Users Service
 * @version 1.0
 * @see com.simdev.api_users.shared.exception.GlobalExceptionHandler
 */
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String resourceName, Long id) {
        super(resourceName + " non trouvé avec l'ID : " + id);
    }
    
    public ResourceNotFoundException(String message) {
        super(message);
    }
}

