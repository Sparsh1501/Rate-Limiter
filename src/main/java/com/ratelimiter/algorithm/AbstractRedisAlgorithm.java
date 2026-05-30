package com.ratelimiter.algorithm;

import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.service.RateLimiterMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

/**
 * Base class sharing Lua-script execution, latency metrics and the fail-open
 * fallback strategy for the Redis-backed algorithms.
 */
@Slf4j
public abstract class AbstractRedisAlgorithm implements RateLimiterAlgorithm {

    protected final StringRedisTemplate redisTemplate;
    protected final RateLimiterMetrics metrics;

    /**
     * @param redisTemplate the Redis string template
     * @param metrics       the metrics recorder
     */
    protected AbstractRedisAlgorithm(StringRedisTemplate redisTemplate, RateLimiterMetrics metrics) {
        this.redisTemplate = redisTemplate;
        this.metrics = metrics;
    }

    /** {@inheritDoc} */
    @Override
    public boolean isAllowed(String merchantId, com.ratelimiter.model.RateLimitConfig config) {
        return evaluate(merchantId, config).isAllowed();
    }

    /**
     * Executes a rate limiting Lua script, timing the Redis round trip and
     * translating the {@code [allowed, remaining, reset, limit]} reply into a
     * {@link RateLimitResult}. On any Redis failure the request fails open.
     *
     * @param script  the script to execute
     * @param keys    the Redis keys the script operates on
     * @param limit   the configured limit (used for the fail-open result)
     * @param args    the script arguments
     * @return the parsed result, or a fail-open result on error
     */
    @SuppressWarnings("rawtypes")
    protected RateLimitResult execute(RedisScript<List> script, List<String> keys, long limit, String... args) {
        long start = System.nanoTime();
        try {
            List raw = redisTemplate.execute(script, keys, (Object[]) args);
            metrics.recordRedisLatency(System.nanoTime() - start);
            return parse(raw);
        } catch (RuntimeException ex) {
            metrics.recordRedisLatency(System.nanoTime() - start);
            log.error("Redis unavailable for algorithm {} — failing open: {}", getType(), ex.getMessage());
            return RateLimitResult.failOpen(limit, getType());
        }
    }

    @SuppressWarnings("rawtypes")
    private RateLimitResult parse(List raw) {
        if (raw == null || raw.size() < 4) {
            return RateLimitResult.failOpen(0, getType());
        }
        long allowed = toLong(raw.get(0));
        long remaining = toLong(raw.get(1));
        long reset = toLong(raw.get(2));
        long limit = toLong(raw.get(3));
        return RateLimitResult.builder()
                .allowed(allowed == 1)
                .remaining(Math.max(0, remaining))
                .resetEpochSeconds(reset)
                .limit(limit)
                .algorithm(getType())
                .build();
    }

    private long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
