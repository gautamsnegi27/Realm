package com.gn.reminder.userservice.shared.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Swagger/OpenAPI 3 Configuration for User Service
 * Provides comprehensive API documentation with JWT authentication support
 */
@Configuration
public class SwaggerConfig {

    @Value("${server.port:8891}")
    private String serverPort;

    @Value("${spring.application.name:user-service}")
    private String applicationName;

    @Bean
    public OpenAPI userServiceOpenAPI() {
        return new OpenAPI()
                .info(apiInfo())
                .servers(serverList())
                .addSecurityItem(securityRequirement())
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer Authentication", securityScheme()));
    }

    private Info apiInfo() {
        return new Info()
                .title("User Service API")
                .description("""
                        **User Management & Authentication Service**

                        This service provides comprehensive user management capabilities including:
                        - User registration and authentication
                        - JWT token-based security
                        - OAuth2/Social login integration (Google, GitHub)
                        - User profile management
                        - Caching with Redis
                        - MongoDB data persistence

                        **Authentication:**
                        - Most endpoints require JWT authentication
                        - Use `/api/v1/auth/login` or `/api/v1/auth/signup` to get a token
                        - Include token in Authorization header: `Bearer <your-token>`

                        **Rate Limiting:**
                        - API Gateway enforces rate limits (100 requests/minute per user)
                        - Unauthenticated requests are limited by IP address
                        """)
                .version("1.0.0")
                .contact(contact())
                .license(license());
    }

    private Contact contact() {
        return new Contact()
                .name("Development Team")
                .email("dev@reminder.gn.com")
                .url("https://github.com/gn413l/Realm");
    }

    private License license() {
        return new License()
                .name("MIT License")
                .url("https://opensource.org/licenses/MIT");
    }

    private List<Server> serverList() {
        return List.of(
                new Server()
                        .url("http://localhost:" + serverPort)
                        .description("Local Development Server"),
                new Server()
                        .url("http://localhost:8111")
                        .description("API Gateway (Recommended)")
        );
    }

    private SecurityRequirement securityRequirement() {
        return new SecurityRequirement().addList("Bearer Authentication");
    }

    private SecurityScheme securityScheme() {
        return new SecurityScheme()
                .name("Bearer Authentication")
                .type(SecurityScheme.Type.HTTP)
                .scheme("bearer")
                .bearerFormat("JWT")
                .description("Enter JWT token obtained from login/signup endpoints");
    }
}
