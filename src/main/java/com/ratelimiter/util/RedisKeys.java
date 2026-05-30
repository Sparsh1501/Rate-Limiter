package com.ratelimiter.util;

/**
 * Central definition of the Redis key naming scheme.
 *
 * <pre>
 * ratelimiter:merchant:{merchantId}:config          # Merchant config hash
 * ratelimiter:merchant:{merchantId}:fixedwindow     # Fixed window counter (with TTL)
 * ratelimiter:merchant:{merchantId}:slidingwindow   # Sorted set of timestamps
 * ratelimiter:merchant:{merchantId}:tokenbucket     # Token bucket hash {tokens, lastRefill}
 * </pre>
 */
public final class RedisKeys {

    private static final String PREFIX = "ratelimiter:merchant:";

    private RedisKeys() {
    }

    /**
     * @param merchantId the merchant id
     * @return key for the merchant configuration hash
     */
    public static String config(String merchantId) {
        return PREFIX + merchantId + ":config";
    }

    /**
     * @param merchantId the merchant id
     * @return key for the fixed window counter
     */
    public static String fixedWindow(String merchantId) {
        return PREFIX + merchantId + ":fixedwindow";
    }

    /**
     * @param merchantId the merchant id
     * @return key for the sliding window sorted set
     */
    public static String slidingWindow(String merchantId) {
        return PREFIX + merchantId + ":slidingwindow";
    }

    /**
     * @param merchantId the merchant id
     * @return key for the token bucket hash
     */
    public static String tokenBucket(String merchantId) {
        return PREFIX + merchantId + ":tokenbucket";
    }
}
