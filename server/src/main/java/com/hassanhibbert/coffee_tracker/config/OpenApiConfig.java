package com.hassanhibbert.coffee_tracker.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;
import java.util.regex.Pattern;

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

    private static final Pattern RESPONSE_SCHEMA = Pattern.compile(".*(Response|Summary|Stat)$");

    /**
     * Jackson always writes every field of a response record (null ones as null), but
     * springdoc marks them all optional. Declaring them required makes generated clients
     * (the TypeScript types in client/) say "id: number" instead of "id?: number".
     * Fields that really can be null carry @Schema(nullable = true) on the record.
     */
    @Bean
    OpenApiCustomizer responseFieldsAreAlwaysPresent() {
        return openApi -> openApi.getComponents().getSchemas().forEach((name, schema) -> {
            if (RESPONSE_SCHEMA.matcher(name).matches() && schema.getProperties() != null) {
                schema.setRequired(new ArrayList<>(schema.getProperties().keySet()));
            }
        });
    }
}
