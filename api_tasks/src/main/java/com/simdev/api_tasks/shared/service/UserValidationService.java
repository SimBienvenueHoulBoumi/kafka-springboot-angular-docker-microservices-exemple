package com.simdev.api_tasks.shared.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Service de validation des utilisateurs utilisant le cache.
 * <p>
 * Service de compatibilité qui délègue à UserCacheService.
 * Conservé pour la compatibilité ascendante si nécessaire.
 * </p>
 *
 * @deprecated Utiliser UserCacheService directement qui inclut le cache
 * @author API Tasks Service
 * @version 1.0
 */
@Service
@Slf4j
@RequiredArgsConstructor
@Deprecated
public class UserValidationService {
    
    private final UserCacheService userCacheService;
    
    /**
     * Valide qu'un utilisateur existe.
     *
     * @param userId L'identifiant unique de l'utilisateur à valider
     */
    public void validateUserExists(Long userId) {
        userCacheService.userExists(userId);
    }
}

