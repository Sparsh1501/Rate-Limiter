package com.ratelimiter.repository;

import com.ratelimiter.model.AlgorithmType;
import com.ratelimiter.model.RateLimitConfig;
import com.ratelimiter.util.RedisKeys;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;

/**
 * Persistence of per-merchant {@link RateLimitConfig} as Redis hashes.
 *
 * <p>All operations are wrapped so that Redis connectivity failures are surfaced
 * cleanly to callers (which then apply the fail-open strategy).</p>
 */
@Slf4j
@Repository
@RequiredArgsConstructor
public class RateLimitConfigRepository {

    private static final String FIELD_ALGORITHM = "algorithm";
    private static final String FIELD_REQUEST_LIMIT = "requestLimit";
    private static final String FIELD_WINDOW_SIZE = "windowSizeSeconds";
    private static final String FIELD_TOKENS_PER_SECOND = "tokensPerSecond";

    private final StringRedisTemplate redisTemplate;

    /**
     * Persists (creates or overwrites) a merchant configuration.
     *
     * @param config the configuration to save
     */
    public void save(RateLimitConfig config) {
        String key = RedisKeys.config(config.getMerchantId());
        Map<String, String> hash = Map.of(
                FIELD_ALGORITHM, config.getAlgorithm().name(),
                FIELD_REQUEST_LIMIT, String.valueOf(config.getRequestLimit()),
                FIELD_WINDOW_SIZE, String.valueOf(config.getWindowSizeSeconds()),
                FIELD_TOKENS_PER_SECOND, String.valueOf(config.getTokensPerSecond())
        );
        redisTemplate.opsForHash().putAll(key, hash);
    }

    /**
     * Loads a merchant configuration if present.
     *
     * @param merchantId the merchant id
     * @return the configuration, or empty if none exists
     */
    public Optional<RateLimitConfig> findById(String merchantId) {
        String key = RedisKeys.config(merchantId);
        Map<Object, Object> raw = redisTemplate.opsForHash().entries(key);
        if (raw.isEmpty()) {
            return Optional.empty();
        }
        try {
            RateLimitConfig config = RateLimitConfig.builder()
                    .merchantId(merchantId)
                    .algorithm(AlgorithmType.valueOf(String.valueOf(raw.get(FIELD_ALGORITHM))))
                    .requestLimit(Integer.parseInt(String.valueOf(raw.get(FIELD_REQUEST_LIMIT))))
                    .windowSizeSeconds(Integer.parseInt(String.valueOf(raw.get(FIELD_WINDOW_SIZE))))
                    .tokensPerSecond(Double.parseDouble(String.valueOf(raw.get(FIELD_TOKENS_PER_SECOND))))
                    .build();
            return Optional.of(config);
        } catch (IllegalArgumentException ex) {
            log.warn("Corrupt config for merchant {}: {}", merchantId, ex.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Deletes a merchant configuration.
     *
     * @param merchantId the merchant id
     * @return {@code true} if a configuration was removed
     */
    public boolean deleteById(String merchantId) {
        Boolean deleted = redisTemplate.delete(RedisKeys.config(merchantId));
        return Boolean.TRUE.equals(deleted);
    }

    /**
     * Indicates whether Redis is currently reachable.
     *
     * @return {@code true} if a PING succeeds
     */
    public boolean isHealthy() {
        try {
            return "PONG".equalsIgnoreCase(
                    redisTemplate.execute((org.springframework.data.redis.core.RedisCallback<String>)
                            connection -> connection.ping()));
        } catch (DataAccessException ex) {
            return false;
        }
    }
}
