package com.ecommerce.product.config;

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
 * Конфигурация OpenAPI/Swagger
 * Настраивает документацию API
 */
@Configuration
public class OpenApiConfig {

    @Value("${server.port:8082}")
    private String serverPort;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                // Информация об API
                .info(new Info()
                        .title("Product Service API")
                        .version("1.0.0")
                        .description("REST API для управления товарами в e-commerce платформе. " +
                                "Поддерживает CRUD операции, поиск, фильтрацию и управление запасами.")
                        .contact(new Contact()
                                .name("E-commerce Team")
                                .email("support@ecommerce.com")
                                .url("https://ecommerce.com")
                        )
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")
                        )
                )

                // Серверы
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local development server"),
                        new Server()
                                .url("https://api.ecommerce.com")
                                .description("Production server")
                ))

                // Security схема JWT
                .addSecurityItem(new SecurityRequirement()
                        .addList("Bearer JWT")
                )
                .components(new io.swagger.v3.oas.models.Components()
                        .addSecuritySchemes("Bearer JWT",
                                new SecurityScheme()
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("JWT токен от auth-service. " +
                                                "Получите токен через POST /api/auth/login и используйте в заголовке: " +
                                                "Authorization: Bearer <token>")
                        )
                );
    }
}
