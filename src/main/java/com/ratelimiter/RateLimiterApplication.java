package com.ratelimiter;

import com.ratelimiter.config.RateLimiterProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

/**
 * Entry point for the Distributed Rate Limiter microservice.
 *
 * <p>This service provides a standalone, Redis-backed rate limiting solution that
 * supports multiple algorithms (Fixed Window, Sliding Window Log and Token Bucket)
 * with per-merchant configuration. It is intended to sit in front of an API gateway
 * or be embedded as a shared rate limiting component.</p>
 */
@SpringBootApplication
@EnableConfigurationProperties(RateLimiterProperties.class)
public class RateLimiterApplication {

    /**
     * Boots the Spring application context.
     *
     * @param args standard command line arguments
     */
    public static void main(String[] args) {
        SpringApplication.run(RateLimiterApplication.class, args);
    }
}
