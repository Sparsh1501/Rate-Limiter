package com.ratelimiter.exception;

/**
 * Thrown when a supplied rate limit configuration is invalid. Maps to HTTP 400.
 */
public class InvalidConfigException extends RuntimeException {

    /**
     * @param message detail describing why the configuration is invalid
     */
    public InvalidConfigException(String message) {
        super(message);
    }
}
