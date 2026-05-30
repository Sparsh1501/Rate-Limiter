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
import java.util.UUID;

/**
 * Sliding Window Log algorithm.
 *
 * <p>Stores one entry per request in a Redis sorted set scored by timestamp.
 * Each evaluation atomically trims expired entries, counts the survivors and
 * conditionally records the new request.</p>
 */
@Component
public class SlidingWindowRateLimiter extends AbstractRedisAlgorithm {

    @SuppressWarnings("rawtypes")
    private final RedisScript<List> script;

    /**
     * @param redisTemplate       the Redis template
     * @param metrics             the metrics recorder
     * @param slidingWindowScript the sliding window Lua script
     */
    @SuppressWarnings("rawtypes")
    public SlidingWindowRateLimiter(StringRedisTemplate redisTemplate,
                                    RateLimiterMetrics metrics,
                                    @Qualifier("slidingWindowScript") RedisScript<List> slidingWindowScript) {
        super(redisTemplate, metrics);
        this.script = slidingWindowScript;
    }

    /** {@inheritDoc} */
    @Override
    public RateLimitResult evaluate(String merchantId, RateLimitConfig config) {
        long nowMillis = Instant.now().toEpochMilli();
        String member = nowMillis + ":" + UUID.randomUUID();
        return execute(
                script,
                List.of(RedisKeys.slidingWindow(merchantId)),
                config.getRequestLimit(),
                String.valueOf(config.getRequestLimit()),
                String.valueOf(config.getWindowSizeSeconds()),
                String.valueOf(nowMillis),
                member);
    }

    /** {@inheritDoc} */
    @Override
    public RateLimitResult peek(String merchantId, RateLimitConfig config) {
        String key = RedisKeys.slidingWindow(merchantId);
        long nowMillis = Instant.now().toEpochMilli();
        long windowStart = nowMillis - (config.getWindowSizeSeconds() * 1000L);
        Long count = redisTemplate.opsForZSet().count(key, windowStart, Double.POSITIVE_INFINITY);
        long used = count == null ? 0 : count;
        return RateLimitResult.builder()
                .allowed(used < config.getRequestLimit())
                .limit(config.getRequestLimit())
                .remaining(Math.max(0, config.getRequestLimit() - used))
                .resetEpochSeconds(Instant.now().getEpochSecond() + config.getWindowSizeSeconds())
                .algorithm(getType())
                .build();
    }

    /** {@inheritDoc} */
    @Override
    public AlgorithmType getType() {
        return AlgorithmType.SLIDING_WINDOW;
    }
}
