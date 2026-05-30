package com.ratelimiter.service;

import com.ratelimiter.algorithm.RateLimiterAlgorithm;
import com.ratelimiter.model.RateLimitConfig;
import com.ratelimiter.model.RateLimitResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Orchestrates rate limit evaluation: resolves the merchant's effective
 * configuration, selects the configured algorithm, records metrics and logs
 * blocked requests.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RateLimitService {

    private final MerchantConfigService configService;
    private final AlgorithmRegistry algorithmRegistry;
    private final RateLimiterMetrics metrics;

    /**
     * Evaluates a single request for the given merchant.
     *
     * @param merchantId the calling merchant id
     * @return the evaluation result containing quota header data
     */
    public RateLimitResult check(String merchantId) {
        RateLimitConfig config = configService.resolveEffectiveConfig(merchantId);
        RateLimiterAlgorithm algorithm = algorithmRegistry.get(config.getAlgorithm());

        RateLimitResult result = algorithm.evaluate(merchantId, config);
        metrics.recordRequest(merchantId, config.getAlgorithm(), result.isAllowed());

        if (!result.isAllowed()) {
            log.warn("Rate limit BLOCKED merchant={} algorithm={} limit={} resetAt={}",
                    merchantId, config.getAlgorithm(), result.getLimit(), result.getResetEpochSeconds());
        }
        return result;
    }

    /**
     * Returns a non-mutating usage snapshot for the merchant.
     *
     * @param merchantId the merchant id
     * @return the current usage result
     */
    public RateLimitResult status(String merchantId) {
        RateLimitConfig config = configService.resolveEffectiveConfig(merchantId);
        RateLimiterAlgorithm algorithm = algorithmRegistry.get(config.getAlgorithm());
        return algorithm.peek(merchantId, config);
    }
}
