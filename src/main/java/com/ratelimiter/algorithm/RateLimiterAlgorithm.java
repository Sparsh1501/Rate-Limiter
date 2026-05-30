package com.ratelimiter.algorithm;

import com.ratelimiter.model.AlgorithmType;
import com.ratelimiter.model.RateLimitConfig;
import com.ratelimiter.model.RateLimitResult;

/**
 * Common contract for all rate limiting algorithms.
 *
 * <p>Implementations must be safe for concurrent use and must perform their
 * read-modify-write logic atomically (via Redis Lua scripts).</p>
 */
public interface RateLimiterAlgorithm {

    /**
     * Evaluates whether a single request for the given merchant is allowed.
     *
     * @param merchantId the merchant identifier
     * @param config     the effective configuration for the merchant
     * @return {@code true} if the request is permitted
     */
    boolean isAllowed(String merchantId, RateLimitConfig config);

    /**
     * Evaluates a request and returns the full result (including quota headers).
     *
     * @param merchantId the merchant identifier
     * @param config     the effective configuration for the merchant
     * @return the evaluation result
     */
    RateLimitResult evaluate(String merchantId, RateLimitConfig config);

    /**
     * Computes a non-mutating usage snapshot for the merchant.
     *
     * @param merchantId the merchant identifier
     * @param config     the effective configuration for the merchant
     * @return a peek at current usage without consuming quota
     */
    RateLimitResult peek(String merchantId, RateLimitConfig config);

    /**
     * @return the algorithm type implemented by this class
     */
    AlgorithmType getType();
}
