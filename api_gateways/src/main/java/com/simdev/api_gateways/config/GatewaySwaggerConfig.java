package com.simdev.api_gateways.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import io.swagger.v3.oas.models.responses.ApiResponses;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration pour générer la documentation Swagger manuellement
 * basée sur les routes Gateway configurées
 */
@Configuration
public class GatewaySwaggerConfig {
    
    @Bean
    public GroupedOpenApi allApis() {
        return GroupedOpenApi.builder()
                .group("all")
                .displayName("Toutes les APIs")
                .addOpenApiCustomizer(openApi -> {
                    openApi.setPaths(createPaths());
                })
                .build();
    }
    
    public Paths createPaths() {
        Paths paths = new Paths();
        
        // === Authentication Routes ===
        paths.addPathItem("/api/auth/register", createRegisterPath());
        paths.addPathItem("/api/auth/login", createLoginPath());
        
        // === Users Service Routes ===
        paths.addPathItem("/api/users", createUsersListPath());
        paths.addPathItem("/api/users/me", createMyUserPath());
        paths.addPathItem("/api/users/{id}", createUserByIdPath());
        
        // === Tasks Service Routes ===
        paths.addPathItem("/api/tasks", createTasksListPath());
        paths.addPathItem("/api/tasks/{id}", createTaskByIdPath());
        
        return paths;
    }
    
    // Authentication endpoints
    private PathItem createRegisterPath() {
        PathItem pathItem = new PathItem();
        
        // POST /api/auth/register
        ApiResponses registerResponses = new ApiResponses();
        registerResponses.addApiResponse("201", new ApiResponse()
                .description("User registered successfully")
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .schema(new Schema().$ref("#/components/schemas/UserResponse")))));
        registerResponses.addApiResponse("400", new ApiResponse().description("Bad Request - User already exists or invalid data"));
        registerResponses.addApiResponse("500", new ApiResponse().description("Internal Server Error"));
        
        pathItem.setPost(new Operation()
                .summary("Register a new user")
                .operationId("register")
                .description("Create a new user account with email, password and personal information")
                .tags(List.of("Authentication"))
                .requestBody(createRequestBody("RegisterRequest", "User registration data"))
                .responses(registerResponses));
        
        return pathItem;
    }
    
    private PathItem createLoginPath() {
        PathItem pathItem = new PathItem();
        
        // POST /api/auth/login
        ApiResponses loginResponses = new ApiResponses();
        loginResponses.addApiResponse("200", new ApiResponse()
                .description("Login successful")
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .schema(new Schema().$ref("#/components/schemas/LoginResponse")))));
        loginResponses.addApiResponse("400", new ApiResponse().description("Invalid credentials"));
        loginResponses.addApiResponse("401", new ApiResponse().description("Unauthorized"));
        loginResponses.addApiResponse("500", new ApiResponse().description("Internal Server Error"));
        
        pathItem.setPost(new Operation()
                .summary("Login user")
                .operationId("login")
                .description("Authenticate user with email and password, returns JWT token")
                .tags(List.of("Authentication"))
                .requestBody(createRequestBody("LoginRequest", "Login credentials"))
                .responses(loginResponses));
        
        return pathItem;
    }
    
    // Users endpoints
    private PathItem createUsersListPath() {
        PathItem pathItem = new PathItem();
        SecurityRequirement securityRequirement = createSecurityRequirement();
        
        // GET /api/users (Admin only)
        pathItem.setGet(new Operation()
                .summary("Get all users (Admin only)")
                .operationId("getAllUsers")
                .description("Seul un administrateur peut consulter la liste de tous les utilisateurs. Les utilisateurs normaux doivent utiliser /api/users/me pour consulter leur propre profil.")
                .tags(List.of("Users (Admin)"))
                .security(List.of(securityRequirement))
                .responses(createSuccessResponse("List of users", "array", "#/components/schemas/UserResponse")));
        
        // POST /api/users (Admin only)
        pathItem.setPost(new Operation()
                .summary("Create a new user (Admin only)")
                .operationId("createUser")
                .description("Seul un administrateur peut créer un nouvel utilisateur. Les utilisateurs normaux doivent utiliser /api/auth/register pour s'enregistrer.")
                .tags(List.of("Users (Admin)"))
                .security(List.of(securityRequirement))
                .requestBody(createRequestBody("UserRequest", "User data to create"))
                .responses(createCreatedResponse("User created", "UserResponse")));
        
        return pathItem;
    }
    
    private PathItem createMyUserPath() {
        PathItem pathItem = new PathItem();
        SecurityRequirement securityRequirement = createSecurityRequirement();
        
        // GET /api/users/me
        pathItem.setGet(new Operation()
                .summary("Get current user profile")
                .operationId("getMyProfile")
                .description("Permet à un utilisateur authentifié de consulter son propre profil. Accessible à tous les utilisateurs authentifiés.")
                .tags(List.of("Users"))
                .security(List.of(securityRequirement))
                .responses(createSuccessResponse("Current user details", "UserResponse")));
        
        // PUT /api/users/me
        pathItem.setPut(new Operation()
                .summary("Update current user profile")
                .operationId("updateMyProfile")
                .description("Permet à un utilisateur authentifié de modifier son propre profil. Accessible à tous les utilisateurs authentifiés.")
                .tags(List.of("Users"))
                .security(List.of(securityRequirement))
                .requestBody(createRequestBody("UserRequest", "User data to update"))
                .responses(createSuccessResponse("User updated", "UserResponse")));
        
        // DELETE /api/users/me
        pathItem.setDelete(new Operation()
                .summary("Delete current user account")
                .operationId("deleteMyAccount")
                .description("Permet à un utilisateur authentifié de supprimer son propre compte. Accessible à tous les utilisateurs authentifiés.")
                .tags(List.of("Users"))
                .security(List.of(securityRequirement))
                .responses(createNoContentResponse()));
        
        return pathItem;
    }
    
    private PathItem createUserByIdPath() {
        PathItem pathItem = new PathItem();
        SecurityRequirement securityRequirement = createSecurityRequirement();
        
        // GET /api/users/{id} (Admin only)
        pathItem.setGet(new Operation()
                .summary("Get user by ID (Admin only)")
                .operationId("getUserById")
                .description("Seul un administrateur peut consulter le profil d'un autre utilisateur. Utilisez /api/users/me pour consulter votre propre profil.")
                .tags(List.of("Users (Admin)"))
                .security(List.of(securityRequirement))
                .parameters(List.of(createPathParameter("id", "User ID")))
                .responses(createSuccessResponse("User details", "UserResponse")));
        
        // PUT /api/users/{id} (Admin only)
        pathItem.setPut(new Operation()
                .summary("Update user (Admin only)")
                .operationId("updateUser")
                .description("Seul un administrateur peut modifier le profil d'un autre utilisateur. Utilisez /api/users/me pour modifier votre propre profil.")
                .tags(List.of("Users (Admin)"))
                .security(List.of(securityRequirement))
                .parameters(List.of(createPathParameter("id", "User ID")))
                .requestBody(createRequestBody("UserRequest", "User data to update"))
                .responses(createSuccessResponse("User updated", "UserResponse")));
        
        // DELETE /api/users/{id} (Admin only)
        pathItem.setDelete(new Operation()
                .summary("Delete user (Admin only)")
                .operationId("deleteUser")
                .description("Seul un administrateur peut supprimer le compte d'un autre utilisateur. Utilisez /api/users/me pour supprimer votre propre compte.")
                .tags(List.of("Users (Admin)"))
                .security(List.of(securityRequirement))
                .parameters(List.of(createPathParameter("id", "User ID")))
                .responses(createNoContentResponse()));
        
        return pathItem;
    }
    
    // Tasks endpoints
    private PathItem createTasksListPath() {
        PathItem pathItem = new PathItem();
        SecurityRequirement securityRequirement = createSecurityRequirement();
        
        // GET /api/tasks
        pathItem.setGet(new Operation()
                .summary("Get current user's tasks")
                .operationId("getAllTasks")
                .description("Retourne toutes les tâches de l'utilisateur connecté (userId extrait automatiquement du token JWT)")
                .tags(List.of("Tasks"))
                .security(List.of(securityRequirement))
                .responses(createSuccessResponse("List of tasks", "array", "#/components/schemas/TaskResponse")));
        
        // POST /api/tasks
        pathItem.setPost(new Operation()
                .summary("Create a new task")
                .description("Crée une nouvelle tâche pour l'utilisateur connecté (userId extrait automatiquement du token JWT)")
                .operationId("createTask")
                .tags(List.of("Tasks"))
                .security(List.of(securityRequirement))
                .requestBody(createRequestBody("TaskRequest", "Task data to create (userId n'est pas requis, il est extrait du token)"))
                .responses(createCreatedResponse("Task created", "TaskResponse")));
        
        return pathItem;
    }
    
    private PathItem createTaskByIdPath() {
        PathItem pathItem = new PathItem();
        SecurityRequirement securityRequirement = createSecurityRequirement();
        
        // GET /api/tasks/{id}
        pathItem.setGet(new Operation()
                .summary("Get task by ID")
                .operationId("getTaskById")
                .tags(List.of("Tasks"))
                .security(List.of(securityRequirement))
                .parameters(List.of(createPathParameter("id", "Task ID")))
                .responses(createSuccessResponse("Task details", "TaskResponse")));
        
        // PUT /api/tasks/{id}
        pathItem.setPut(new Operation()
                .summary("Update task")
                .operationId("updateTask")
                .tags(List.of("Tasks"))
                .security(List.of(securityRequirement))
                .parameters(List.of(createPathParameter("id", "Task ID")))
                .requestBody(createRequestBody("TaskRequest", "Task data to update"))
                .responses(createSuccessResponse("Task updated", "TaskResponse")));
        
        // DELETE /api/tasks/{id}
        pathItem.setDelete(new Operation()
                .summary("Delete task")
                .operationId("deleteTask")
                .tags(List.of("Tasks"))
                .security(List.of(securityRequirement))
                .parameters(List.of(createPathParameter("id", "Task ID")))
                .responses(createNoContentResponse()));
        
        return pathItem;
    }
    
    
    // Helper methods
    private SecurityRequirement createSecurityRequirement() {
        return new SecurityRequirement().addList("bearer-jwt");
    }
    
    private Parameter createPathParameter(String name, String description) {
        Parameter param = new Parameter();
        param.setName(name);
        param.setIn("path");
        param.setRequired(true);
        param.setDescription(description);
        param.setSchema(new Schema().type("integer").format("int64"));
        return param;
    }
    
    private io.swagger.v3.oas.models.parameters.RequestBody createRequestBody(String schemaRef, String description) {
        io.swagger.v3.oas.models.parameters.RequestBody requestBody = new io.swagger.v3.oas.models.parameters.RequestBody();
        requestBody.setRequired(true);
        requestBody.setDescription(description);
        Content content = new Content();
        MediaType mediaType = new MediaType();
        mediaType.setSchema(new Schema().$ref("#/components/schemas/" + schemaRef));
        content.addMediaType("application/json", mediaType);
        requestBody.setContent(content);
        return requestBody;
    }
    
    private ApiResponses createSuccessResponse(String description, String schemaRef) {
        ApiResponses responses = new ApiResponses();
        responses.addApiResponse("200", new ApiResponse()
                .description(description)
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .schema(new Schema().$ref("#/components/schemas/" + schemaRef)))));
        responses.addApiResponse("400", new ApiResponse().description("Bad Request"));
        responses.addApiResponse("404", new ApiResponse().description("Not Found"));
        responses.addApiResponse("500", new ApiResponse().description("Internal Server Error"));
        return responses;
    }
    
    private ApiResponses createSuccessResponse(String description, String type, String items) {
        ApiResponses responses = new ApiResponses();
        Schema schema = new Schema();
        schema.setType(type);
        if (items != null) {
            schema.setItems(new Schema().$ref(items));
        }
        responses.addApiResponse("200", new ApiResponse()
                .description(description)
                .content(new Content()
                        .addMediaType("application/json", new MediaType().schema(schema))));
        responses.addApiResponse("500", new ApiResponse().description("Internal Server Error"));
        return responses;
    }
    
    private ApiResponses createCreatedResponse(String description, String schemaRef) {
        ApiResponses responses = new ApiResponses();
        responses.addApiResponse("201", new ApiResponse()
                .description(description)
                .content(new Content()
                        .addMediaType("application/json", new MediaType()
                                .schema(new Schema().$ref("#/components/schemas/" + schemaRef)))));
        responses.addApiResponse("400", new ApiResponse().description("Bad Request"));
        responses.addApiResponse("500", new ApiResponse().description("Internal Server Error"));
        return responses;
    }
    
    private ApiResponses createNoContentResponse() {
        ApiResponses responses = new ApiResponses();
        responses.addApiResponse("204", new ApiResponse().description("No Content"));
        responses.addApiResponse("404", new ApiResponse().description("Not Found"));
        responses.addApiResponse("500", new ApiResponse().description("Internal Server Error"));
        return responses;
    }
}

