package com.ratelimiter.service;

import com.ratelimiter.config.RateLimiterProperties;
import com.ratelimiter.dto.RateLimitConfigRequest;
import com.ratelimiter.exception.InvalidConfigException;
import com.ratelimiter.exception.MerchantNotFoundException;
import com.ratelimiter.model.AlgorithmType;
import com.ratelimiter.model.RateLimitConfig;
import com.ratelimiter.repository.RateLimitConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Business logic for managing merchant configurations.
 *
 * <p>Provides CRUD operations, validation, a short-lived in-process cache (to
 * reduce Redis round trips on the hot path) and a default-config fallback.</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MerchantConfigService {

    private final RateLimitConfigRepository repository;
    private final RateLimiterProperties properties;
    private final RateLimiterMetrics metrics;

    private final ConcurrentHashMap<String, CachedConfig> cache = new ConcurrentHashMap<>();

    /**
     * Creates or updates a merchant configuration after validation.
     *
     * @param merchantId the merchant id
     * @param request    the configuration payload
     * @return the persisted configuration
     */
    public RateLimitConfig saveConfig(String merchantId, RateLimitConfigRequest request) {
        validate(merchantId, request);
        RateLimitConfig config = RateLimitConfig.builder()
                .merchantId(merchantId)
                .algorithm(request.getAlgorithm())
                .requestLimit(request.getRequestLimit())
                .windowSizeSeconds(request.getWindowSizeSeconds())
                .tokensPerSecond(request.getTokensPerSecond())
                .build();
        repository.save(config);
        cache.put(merchantId, new CachedConfig(config, now()));
        log.info("Saved config for merchant {}: {}", merchantId, config);
        return config;
    }

    /**
     * Fetches an explicitly configured merchant, failing if none exists.
     *
     * @param merchantId the merchant id
     * @return the stored configuration
     * @throws MerchantNotFoundException if the merchant has no configuration
     */
    public RateLimitConfig getConfig(String merchantId) {
        return repository.findById(merchantId)
                .orElseThrow(() -> new MerchantNotFoundException(merchantId));
    }

    /**
     * Deletes a merchant configuration and evicts it from the cache.
     *
     * @param merchantId the merchant id
     * @throws MerchantNotFoundException if no configuration existed
     */
    public void deleteConfig(String merchantId) {
        boolean deleted = repository.deleteById(merchantId);
        cache.remove(merchantId);
        if (!deleted) {
            throw new MerchantNotFoundException(merchantId);
        }
        log.info("Deleted config for merchant {}", merchantId);
    }

    /**
     * Resolves the effective configuration for a merchant on the hot path,
     * consulting the cache first and falling back to defaults when no explicit
     * configuration exists. Errors are absorbed so the rate limiter can fail open.
     *
     * @param merchantId the merchant id
     * @return the effective configuration (never {@code null})
     */
    public RateLimitConfig resolveEffectiveConfig(String merchantId) {
        CachedConfig cached = cache.get(merchantId);
        if (cached != null && !cached.isExpired(now(), properties.getConfigCacheTtlSeconds())) {
            metrics.recordConfigCache(true);
            return cached.config();
        }
        metrics.recordConfigCache(false);
        try {
            Optional<RateLimitConfig> stored = repository.findById(merchantId);
            RateLimitConfig effective = stored.orElseGet(() -> defaultConfig(merchantId));
            cache.put(merchantId, new CachedConfig(effective, now()));
            return effective;
        } catch (RuntimeException ex) {
            log.warn("Config lookup failed for merchant {} — using defaults: {}", merchantId, ex.getMessage());
            return defaultConfig(merchantId);
        }
    }

    /**
     * Non-blocking variant of {@link #resolveEffectiveConfig(String)}.
     *
     * @param merchantId the merchant id
     * @return a future resolving to the effective configuration
     */
    public CompletableFuture<RateLimitConfig> resolveEffectiveConfigAsync(String merchantId) {
        return CompletableFuture.supplyAsync(() -> resolveEffectiveConfig(merchantId));
    }

    /**
     * Builds the default fallback configuration for an unconfigured merchant.
     *
     * @param merchantId the merchant id
     * @return a default configuration
     */
    public RateLimitConfig defaultConfig(String merchantId) {
        return RateLimitConfig.builder()
                .merchantId(merchantId)
                .algorithm(properties.getDefaultAlgorithm())
                .requestLimit(properties.getDefaultLimit())
                .windowSizeSeconds(properties.getDefaultWindowSeconds())
                .tokensPerSecond(properties.getDefaultTokensPerSecond())
                .build();
    }

    private void validate(String merchantId, RateLimitConfigRequest request) {
        if (merchantId == null || merchantId.isBlank()) {
            throw new InvalidConfigException("merchantId must not be blank");
        }
        if (request.getAlgorithm() == null) {
            throw new InvalidConfigException("algorithm is required");
        }
        if (request.getRequestLimit() <= 0) {
            throw new InvalidConfigException("requestLimit must be positive");
        }
        boolean windowBased = request.getAlgorithm() == AlgorithmType.FIXED_WINDOW
                || request.getAlgorithm() == AlgorithmType.SLIDING_WINDOW;
        if (windowBased && request.getWindowSizeSeconds() <= 0) {
            throw new InvalidConfigException("windowSizeSeconds must be positive for window-based algorithms");
        }
        if (request.getAlgorithm() == AlgorithmType.TOKEN_BUCKET && request.getTokensPerSecond() <= 0) {
            throw new InvalidConfigException("tokensPerSecond must be positive for the token bucket algorithm");
        }
    }

    private long now() {
        return System.currentTimeMillis();
    }

    /** Cache entry holding a config and the time it was stored. */
    private record CachedConfig(RateLimitConfig config, long storedAtMillis) {
        boolean isExpired(long nowMillis, long ttlSeconds) {
            return (nowMillis - storedAtMillis) > ttlSeconds * 1000;
        }
    }
}
