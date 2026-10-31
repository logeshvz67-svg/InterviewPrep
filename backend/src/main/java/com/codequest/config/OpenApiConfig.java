package com.codequest.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Swagger UI at /swagger-ui.html. Click "Authorize" and paste an access token to try secured endpoints. */
@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI codeQuestOpenApi() {
        return new OpenAPI()
                .info(new Info().title("Code Quest API").version("0.1.0")
                        .description("Gamified Java interview-prep backend. All times are UTC (ISO-8601)."))
                .addSecurityItem(new SecurityRequirement().addList("bearer"))
                .components(new Components().addSecuritySchemes("bearer",
                        new SecurityScheme().type(SecurityScheme.Type.HTTP).scheme("bearer").bearerFormat("JWT")));
    }
}
