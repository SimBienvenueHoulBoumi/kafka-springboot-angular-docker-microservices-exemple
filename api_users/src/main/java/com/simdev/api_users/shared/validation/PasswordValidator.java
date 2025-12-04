package com.simdev.api_users.shared.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

/**
 * Validateur personnalisé pour les mots de passe forts
 * Exige au moins 8 caractères, une majuscule, une minuscule, un chiffre
 */
public class PasswordValidator implements ConstraintValidator<StrongPassword, String> {
    
    private static final Pattern PASSWORD_PATTERN = 
        Pattern.compile("^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$");
    
    @Override
    public void initialize(StrongPassword constraintAnnotation) {
    }
    
    @Override
    public boolean isValid(String password, ConstraintValidatorContext context) {
        if (password == null || password.isEmpty()) {
            return true;
        }
        
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                "Le mot de passe doit contenir au moins 8 caractères, une majuscule, une minuscule et un chiffre"
            ).addConstraintViolation();
            return false;
        }
        
        return true;
    }
}

