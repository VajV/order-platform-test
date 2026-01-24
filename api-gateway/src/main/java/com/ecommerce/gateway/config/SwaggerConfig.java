package com.ecommerce.gateway.config;

import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwaggerConfig {

    @Bean
    public List<GroupedOpenApi> apis() {
        return List.of(
                GroupedOpenApi.builder().group("auth").pathsToMatch("/api/auth/**").build(),
                GroupedOpenApi.builder().group("users").pathsToMatch("/api/users/**").build(),
                GroupedOpenApi.builder().group("products").pathsToMatch("/api/products/**").build(),
                GroupedOpenApi.builder().group("orders").pathsToMatch("/api/orders/**").build(),
                GroupedOpenApi.builder().group("inventory").pathsToMatch("/api/inventory/**").build(),
                GroupedOpenApi.builder().group("notifications").pathsToMatch("/api/notifications/**").build()
        );
    }
}
