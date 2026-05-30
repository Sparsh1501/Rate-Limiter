package com.ratelimiter.config;

import com.ratelimiter.model.AlgorithmType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Strongly typed binding for all {@code rate-limiter.*} configuration properties.
 *
 * <p>Using {@link ConfigurationProperties} (rather than scattered {@code @Value}
 * annotations) keeps configuration centralised, validated and easy to test.</p>
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "rate-limiter")
public class RateLimiterProperties {

    /** Default request limit used when a merchant has no explicit configuration. */
    private int defaultLimit = 100;

    /** Default window size, in seconds, for window-based algorithms. */
    private int defaultWindowSeconds = 60;

    /** Default refill rate (tokens per second) for the token bucket algorithm. */
    private double defaultTokensPerSecond = 10;

    /** Default algorithm applied when a merchant has no explicit configuration. */
    private AlgorithmType defaultAlgorithm = AlgorithmType.SLIDING_WINDOW;

    /** HTTP header inspected by the filter to identify the calling merchant. */
    private String merchantHeader = "X-Merchant-ID";

    /** Request path prefixes that bypass rate limiting entirely. */
    private List<String> excludedPaths = List.of("/actuator", "/swagger-ui", "/v3/api-docs");

    /** Time-to-live (seconds) for the in-process merchant config cache. */
    private long configCacheTtlSeconds = 10;
}
