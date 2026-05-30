package com.ratelimiter.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Outcome of a single rate limit evaluation.
 *
 * <p>Carries the data required to populate the standard {@code X-RateLimit-*}
 * response headers.</p>
 */
@Data
@Builder
@AllArgsConstructor
public class RateLimitResult {

    /** Whether the request is permitted. */
    private boolean allowed;

    /** Configured limit (window count or bucket capacity). */
    private long limit;

    /** Remaining quota after this request was evaluated. */
    private long remaining;

    /** Epoch seconds at which the limit/quota resets or fully refills. */
    private long resetEpochSeconds;

    /** Algorithm that produced this result. */
    private AlgorithmType algorithm;

    /**
     * Convenience factory for a fail-open (allowed) result with no quota data.
     *
     * @param limit     the configured limit to advertise
     * @param algorithm the algorithm in effect
     * @return an allowed result
     */
    public static RateLimitResult failOpen(long limit, AlgorithmType algorithm) {
        return RateLimitResult.builder()
                .allowed(true)
                .limit(limit)
                .remaining(limit)
                .resetEpochSeconds(0)
                .algorithm(algorithm)
                .build();
    }
}
