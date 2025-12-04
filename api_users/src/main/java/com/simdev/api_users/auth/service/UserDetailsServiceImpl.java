package com.simdev.api_users.auth.service;

import com.simdev.api_users.user.domain.User;
import com.simdev.api_users.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;

import static org.springframework.security.core.userdetails.User.builder;

/**
 * Implémentation de UserDetailsService pour Spring Security.
 * <p>
 * Ce service charge les détails d'un utilisateur depuis la base de données
 * et les transforme en UserDetails pour l'authentification Spring Security.
 * </p>
 *
 * @author API Users Service
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {
    
    private final UserRepository userRepository;
    
    /**
     * Charge un utilisateur par son email pour l'authentification Spring Security.
     *
     * @param email L'email de l'utilisateur à charger
     * @return Les détails de l'utilisateur avec ses autorisations (rôle)
     * @throws UsernameNotFoundException si l'utilisateur n'existe pas
     */
    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
        
        List<GrantedAuthority> authorities = Collections.singletonList(
                new SimpleGrantedAuthority(user.getRole().name())
        );
        
        return builder()
                .username(user.getEmail())
                .password(user.getPassword())
                .authorities(authorities)
                .accountExpired(false)
                .accountLocked(false)
                .credentialsExpired(false)
                .disabled(!user.getActive())
                .build();
    }
}

