package com.hassanhibbert.coffee_tracker.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI at /swagger-ui.html, raw spec at /v3/api-docs. The bearer scheme adds an
 * "Authorize" button: paste the accessToken from /auth/login and every call sends it.
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(title = "Coffee Tracker API", version = "v1",
                description = "Log coffee brews: beans, equipment, and brew parameters."),
        security = @SecurityRequirement(name = "bearerAuth"))
@SecurityScheme(name = "bearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
