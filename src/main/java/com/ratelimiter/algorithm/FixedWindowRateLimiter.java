package com.ratelimiter.algorithm;

import com.ratelimiter.model.AlgorithmType;
import com.ratelimiter.model.RateLimitConfig;
import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.service.RateLimiterMetrics;
import com.ratelimiter.util.RedisKeys;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

/**
 * Fixed Window counter algorithm.
 *
 * <p>Maintains a single counter per merchant that resets when its TTL expires.
 * The increment + expiry decision is performed atomically by a Lua script.</p>
 */
@Component
public class FixedWindowRateLimiter extends AbstractRedisAlgorithm {

    @SuppressWarnings("rawtypes")
    private final RedisScript<List> script;

    /**
     * @param redisTemplate     the Redis template
     * @param metrics           the metrics recorder
     * @param fixedWindowScript the fixed window Lua script
     */
    @SuppressWarnings("rawtypes")
    public FixedWindowRateLimiter(StringRedisTemplate redisTemplate,
                                  RateLimiterMetrics metrics,
                                  @Qualifier("fixedWindowScript") RedisScript<List> fixedWindowScript) {
        super(redisTemplate, metrics);
        this.script = fixedWindowScript;
    }

    /** {@inheritDoc} */
    @Override
    public RateLimitResult evaluate(String merchantId, RateLimitConfig config) {
        long now = Instant.now().getEpochSecond();
        return execute(
                script,
                List.of(RedisKeys.fixedWindow(merchantId)),
                config.getRequestLimit(),
                String.valueOf(config.getRequestLimit()),
                String.valueOf(config.getWindowSizeSeconds()),
                String.valueOf(now));
    }

    /** {@inheritDoc} */
    @Override
    public RateLimitResult peek(String merchantId, RateLimitConfig config) {
        String key = RedisKeys.fixedWindow(merchantId);
        String value = redisTemplate.opsForValue().get(key);
        long used = value == null ? 0 : Long.parseLong(value);
        Long ttl = redisTemplate.getExpire(key);
        long reset = Instant.now().getEpochSecond() + (ttl == null || ttl < 0 ? config.getWindowSizeSeconds() : ttl);
        return RateLimitResult.builder()
                .allowed(used < config.getRequestLimit())
                .limit(config.getRequestLimit())
                .remaining(Math.max(0, config.getRequestLimit() - used))
                .resetEpochSeconds(reset)
                .algorithm(getType())
                .build();
    }

    /** {@inheritDoc} */
    @Override
    public AlgorithmType getType() {
        return AlgorithmType.FIXED_WINDOW;
    }
}
