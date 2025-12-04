package com.simdev.api_users.auth.exception;

/**
 * Exception levée lorsque les identifiants d'authentification sont invalides.
 * <p>
 * Cette exception est levée lors de la tentative de connexion lorsque :
 * <ul>
 *   <li>L'email fourni n'existe pas dans le système</li>
 *   <li>Le mot de passe fourni ne correspond pas au mot de passe de l'utilisateur</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Sécurité :</strong>
 * Pour des raisons de sécurité, cette exception ne révèle pas si l'email existe ou non.
 * Le message est générique : "Email ou mot de passe incorrect".
 * </p>
 * <p>
 * <strong>Gestion :</strong>
 * Cette exception est interceptée par {@link com.simdev.api_users.shared.exception.GlobalExceptionHandler}
 * et transformée en réponse HTTP 401 (UNAUTHORIZED).
 * </p>
 * <p>
 * <strong>Impact sur le compte :</strong>
 * Chaque échec d'authentification incrémente le compteur {@code failedLoginAttempts}.
 * Après 5 tentatives échouées, le compte est verrouillé pour 15 minutes.
 * </p>
 *
 * @author API Users Service
 * @version 1.0
 * @see com.simdev.api_users.auth.service.AuthService
 * @see com.simdev.api_users.shared.exception.GlobalExceptionHandler
 */

public class BadCredentialsException extends RuntimeException {
    public BadCredentialsException() {
        super("Email ou mot de passe incorrect");
    }
    
    public BadCredentialsException(String message) {
        super(message);
    }
}

