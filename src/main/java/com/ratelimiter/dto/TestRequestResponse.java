package com.ratelimiter.dto;

import com.ratelimiter.model.AlgorithmType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

/**
 * Result returned by the test endpoint that simulates a single rate-limited request.
 */
@Data
@Builder
@AllArgsConstructor
public class TestRequestResponse {

    private String merchantId;
    private boolean allowed;
    private AlgorithmType algorithm;
    private long limit;
    private long remaining;
    private long resetEpochSeconds;
    private String message;
}
