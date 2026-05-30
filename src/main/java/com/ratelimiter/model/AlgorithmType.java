package com.ratelimiter.model;

/**
 * Supported rate limiting algorithms.
 */
public enum AlgorithmType {

    /** Counts requests in discrete, fixed-size time windows. */
    FIXED_WINDOW,

    /** Keeps a log of request timestamps in a sliding window. */
    SLIDING_WINDOW,

    /** Refills tokens at a steady rate and consumes one per request. */
    TOKEN_BUCKET
}
