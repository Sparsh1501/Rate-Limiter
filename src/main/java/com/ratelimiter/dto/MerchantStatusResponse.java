package com.ratelimiter.dto;

import com.ratelimiter.model.AlgorithmType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Snapshot of a merchant's current rate limit usage.
 */
@Data
@Builder
@AllArgsConstructor
public class MerchantStatusResponse {

    private String merchantId;
    private AlgorithmType algorithm;
    private long limit;
    private long used;
    private long remaining;
    private long resetEpochSeconds;
}
