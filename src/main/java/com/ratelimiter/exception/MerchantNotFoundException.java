package com.ratelimiter.exception;

/**
 * Thrown when no configuration exists for the requested merchant. Maps to HTTP 404.
 */
public class MerchantNotFoundException extends RuntimeException {

    /**
     * @param merchantId the merchant that could not be found
     */
    public MerchantNotFoundException(String merchantId) {
        super("No rate limit configuration found for merchant: " + merchantId);
    }
}
