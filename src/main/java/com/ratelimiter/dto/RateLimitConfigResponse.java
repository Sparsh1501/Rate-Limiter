package com.ratelimiter.dto;

import com.ratelimiter.model.AlgorithmType;
import com.ratelimiter.model.RateLimitConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Response payload describing a merchant's effective rate limit configuration.
 */
@Data
@Builder
@AllArgsConstructor
public class RateLimitConfigResponse {

    private String merchantId;
    private AlgorithmType algorithm;
    private int requestLimit;
    private int windowSizeSeconds;
    private double tokensPerSecond;

    /**
     * Maps a domain {@link RateLimitConfig} into its response representation.
     *
     * @param config the domain configuration
     * @return the API response DTO
     */
    public static RateLimitConfigResponse from(RateLimitConfig config) {
        return RateLimitConfigResponse.builder()
                .merchantId(config.getMerchantId())
                .algorithm(config.getAlgorithm())
                .requestLimit(config.getRequestLimit())
                .windowSizeSeconds(config.getWindowSizeSeconds())
                .tokensPerSecond(config.getTokensPerSecond())
                .build();
    }
}
