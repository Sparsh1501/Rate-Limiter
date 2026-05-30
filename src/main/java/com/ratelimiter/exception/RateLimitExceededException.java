package com.ratelimiter.exception;

/**
 * Thrown when a request exceeds the configured rate limit. Maps to HTTP 429.
 */
public class RateLimitExceededException extends RuntimeException {

    /**
     * @param message detail describing the breach
     */
    public RateLimitExceededException(String message) {
        super(message);
    }
}
