package com.ratelimiter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * OpenAPI / Swagger documentation metadata.
 */
@Configuration
public class OpenApiConfig {

    /**
     * @return the OpenAPI definition served at {@code /swagger-ui.html}
     */
    @Bean
    public OpenAPI rateLimiterOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Distributed Rate Limiter Service")
                        .description("Redis-backed, multi-algorithm, per-merchant rate limiting microservice.")
                        .version("1.0.0")
                        .contact(new Contact().name("Platform Team"))
                        .license(new License().name("Apache 2.0")));
    }
}
