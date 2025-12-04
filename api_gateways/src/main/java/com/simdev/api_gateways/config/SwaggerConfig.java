package com.simdev.api_gateways.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Arrays;
import java.util.List;

@Configuration
public class SwaggerConfig {
    
    @Autowired
    private GatewaySwaggerConfig gatewaySwaggerConfig;
    
    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Microservices API Gateway")
                        .version("1.0.0")
                        .description("API Gateway unifié pour tester tous les microservices (Users et Tasks). " +
                                "Toutes les requêtes passent par le port 8080 avec le préfixe /api")
                        .contact(new Contact()
                                .name("API Gateway")
                                .email("support@example.com"))
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")))
                .servers(List.of(
                        new Server()
                                .url("http://localhost:8080")
                                .description("API Gateway - Environnement de développement")
                ))
                .paths(gatewaySwaggerConfig.createPaths())
                .components(new Components()
                        .addSecuritySchemes("bearer-jwt", new SecurityScheme()
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("Entrez votre token JWT obtenu après connexion. Format: Bearer <token>"))
                        .addSchemas("LoginRequest", createLoginRequestSchema())
                        .addSchemas("LoginResponse", createLoginResponseSchema())
                        .addSchemas("RegisterRequest", createRegisterRequestSchema())
                        .addSchemas("UserRequest", createUserRequestSchema())
                        .addSchemas("UserResponse", createUserResponseSchema())
                        .addSchemas("TaskRequest", createTaskRequestSchema())
                        .addSchemas("TaskResponse", createTaskResponseSchema()));
    }
    
    private Schema<?> createLoginRequestSchema() {
        Schema<?> schema = new Schema<>();
        schema.setType("object");
        schema.setRequired(List.of("email", "password"));
        schema.addProperty("email", new Schema<>().type("string").format("email").example("john.doe@example.com"));
        schema.addProperty("password", new Schema<>().type("string").format("password").example("password123"));
        return schema;
    }
    
    private Schema<?> createLoginResponseSchema() {
        Schema<?> schema = new Schema<>();
        schema.setType("object");
        schema.addProperty("token", new Schema<>().type("string").example("eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."));
        schema.addProperty("type", new Schema<>().type("string").example("Bearer"));
        schema.addProperty("userId", new Schema<>().type("integer").format("int64").example(1L));
        schema.addProperty("email", new Schema<>().type("string").format("email").example("john.doe@example.com"));
        schema.addProperty("firstName", new Schema<>().type("string").example("John"));
        schema.addProperty("lastName", new Schema<>().type("string").example("Doe"));
        return schema;
    }
    
    private Schema<?> createRegisterRequestSchema() {
        Schema<?> schema = new Schema<>();
        schema.setType("object");
        schema.setRequired(List.of("email", "firstName", "lastName", "password"));
        schema.addProperty("email", new Schema<>().type("string").format("email").example("john.doe@example.com"));
        schema.addProperty("firstName", new Schema<>().type("string").example("John"));
        schema.addProperty("lastName", new Schema<>().type("string").example("Doe"));
        schema.addProperty("password", new Schema<>().type("string").format("password").example("password123"));
        return schema;
    }
    
    private Schema<?> createUserRequestSchema() {
        Schema<?> schema = new Schema<>();
        schema.setType("object");
        schema.setRequired(List.of("email", "firstName", "lastName"));
        schema.addProperty("email", new Schema<>().type("string").format("email").example("john.doe@example.com"));
        schema.addProperty("firstName", new Schema<>().type("string").example("John"));
        schema.addProperty("lastName", new Schema<>().type("string").example("Doe"));
        schema.addProperty("password", new Schema<>().type("string").format("password").example("password123"));
        return schema;
    }
    
    private Schema<?> createUserResponseSchema() {
        Schema<?> schema = new Schema<>();
        schema.setType("object");
        schema.addProperty("id", new Schema<>().type("integer").format("int64").example(1L));
        schema.addProperty("email", new Schema<>().type("string").format("email").example("john.doe@example.com"));
        schema.addProperty("firstName", new Schema<>().type("string").example("John"));
        schema.addProperty("lastName", new Schema<>().type("string").example("Doe"));
        Schema<String> roleSchema = new Schema<>();
        roleSchema.setType("string");
        roleSchema.setEnum(Arrays.asList("ROLE_USER", "ROLE_ADMIN"));
        roleSchema.setExample("ROLE_USER");
        roleSchema.setDescription("Le rôle de l'utilisateur (ROLE_USER pour un utilisateur normal, ROLE_ADMIN pour un administrateur)");
        schema.addProperty("role", roleSchema);
        schema.addProperty("active", new Schema<>().type("boolean").example(true));
        schema.addProperty("createdAt", new Schema<>().type("string").format("date-time"));
        schema.addProperty("updatedAt", new Schema<>().type("string").format("date-time"));
        return schema;
    }
    
    private Schema<?> createTaskRequestSchema() {
        Schema<?> schema = new Schema<>();
        schema.setType("object");
        schema.setRequired(List.of("title"));
        schema.addProperty("title", new Schema<>().type("string").example("Complete project")
                .description("Le titre de la tâche (3-200 caractères)"));
        schema.addProperty("description", new Schema<>().type("string").example("Finish the microservices architecture")
                .description("La description de la tâche (max 1000 caractères)"));
        Schema<String> statusSchema = new Schema<>();
        statusSchema.setType("string");
        statusSchema.setEnum(Arrays.asList("PENDING", "IN_PROGRESS", "COMPLETED", "CANCELLED"));
        statusSchema.setExample("PENDING");
        statusSchema.setDescription("Le statut de la tâche");
        schema.addProperty("status", statusSchema);
        return schema;
    }
    
    private Schema<?> createTaskResponseSchema() {
        Schema<?> schema = new Schema<>();
        schema.setType("object");
        schema.addProperty("id", new Schema<>().type("integer").format("int64").example(1L));
        schema.addProperty("title", new Schema<>().type("string").example("Complete project"));
        schema.addProperty("description", new Schema<>().type("string").example("Finish the microservices architecture"));
        schema.addProperty("status", new Schema<>().type("string").example("PENDING"));
        schema.addProperty("userId", new Schema<>().type("integer").format("int64").example(1L));
        schema.addProperty("createdAt", new Schema<>().type("string").format("date-time"));
        schema.addProperty("updatedAt", new Schema<>().type("string").format("date-time"));
        return schema;
    }
}
