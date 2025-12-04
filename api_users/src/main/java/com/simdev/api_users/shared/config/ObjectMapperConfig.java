package com.simdev.api_users.shared.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration de ObjectMapper pour la sérialisation/désérialisation JSON.
 * <p>
 * Cette configuration personnalise le comportement de Jackson ObjectMapper utilisé
 * pour convertir les objets Java en JSON et vice versa dans toute l'application.
 * </p>
 * <p>
 * <strong>Configurations appliquées :</strong>
 * <ul>
 *   <li><strong>JavaTimeModule :</strong> Module pour la sérialisation des types Java 8 Time API
 *       ({@code LocalDateTime}, {@code LocalDate}, etc.) au format ISO-8601</li>
 *   <li><strong>Désactivation des timestamps :</strong> Les dates sont sérialisées en format ISO-8601
 *       (ex: "2025-12-04T14:30:00") plutôt qu'en timestamps numériques</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Usage :</strong>
 * Ce ObjectMapper est utilisé automatiquement par Spring pour :
 * <ul>
 *   <li>Sérialiser les réponses HTTP en JSON</li>
 *   <li>Désérialiser les corps de requêtes JSON en objets Java</li>
 *   <li>Convertir les événements Kafka en JSON et vice versa</li>
 * </ul>
 * </p>
 * <p>
 * <strong>Exemple de sérialisation :</strong>
 * <pre>
 * LocalDateTime date = LocalDateTime.of(2025, 12, 4, 14, 30);
 * // Sérialisé en : "2025-12-04T14:30:00" (pas en timestamp)
 * </pre>
 * </p>
 *
 * @author API Users Service
 * @version 1.0
 * @see ObjectMapper
 * @see JavaTimeModule
 */
@Configuration
public class ObjectMapperConfig {
    
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    }
}

