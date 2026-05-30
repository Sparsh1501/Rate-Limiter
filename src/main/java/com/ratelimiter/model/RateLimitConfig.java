package com.ratelimiter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Per-merchant rate limiting configuration.
 *
 * <p>Persisted in Redis as a hash under
 * {@code ratelimiter:merchant:{merchantId}:config}.</p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RateLimitConfig {

    /** Identifier of the merchant this configuration belongs to. */
    private String merchantId;

    /** Algorithm to apply for this merchant. */
    private AlgorithmType algorithm;

    /** Maximum number of allowed requests per window (or bucket capacity). */
    private int requestLimit;

    /** Window size in seconds for window-based algorithms. */
    private int windowSizeSeconds;

    /** Refill rate (tokens per second) for the token bucket algorithm. */
    private double tokensPerSecond;
}
