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
 * Token Bucket algorithm.
 *
 * <p>Stores the current token count and last refill timestamp in a Redis hash.
 * On each request a Lua script lazily refills tokens based on elapsed time and
 * conditionally consumes one, all atomically.</p>
 */
@Component
public class TokenBucketRateLimiter extends AbstractRedisAlgorithm {

    @SuppressWarnings("rawtypes")
    private final RedisScript<List> script;

    /**
     * @param redisTemplate    the Redis template
     * @param metrics          the metrics recorder
     * @param tokenBucketScript the token bucket Lua script
     */
    @SuppressWarnings("rawtypes")
    public TokenBucketRateLimiter(StringRedisTemplate redisTemplate,
                                  RateLimiterMetrics metrics,
                                  @Qualifier("tokenBucketScript") RedisScript<List> tokenBucketScript) {
        super(redisTemplate, metrics);
        this.script = tokenBucketScript;
    }

    /** {@inheritDoc} */
    @Override
    public RateLimitResult evaluate(String merchantId, RateLimitConfig config) {
        long nowMillis = Instant.now().toEpochMilli();
        double refillRate = effectiveRefillRate(config);
        return execute(
                script,
                List.of(RedisKeys.tokenBucket(merchantId)),
                config.getRequestLimit(),
                String.valueOf(config.getRequestLimit()),
                String.valueOf(refillRate),
                String.valueOf(nowMillis),
                "1");
    }

    /** {@inheritDoc} */
    @Override
    public RateLimitResult peek(String merchantId, RateLimitConfig config) {
        String key = RedisKeys.tokenBucket(merchantId);
        Object tokensRaw = redisTemplate.opsForHash().get(key, "tokens");
        Object lastRefillRaw = redisTemplate.opsForHash().get(key, "lastRefill");
        double capacity = config.getRequestLimit();
        double refillRate = effectiveRefillRate(config);

        double tokens = capacity;
        long nowMillis = Instant.now().toEpochMilli();
        if (tokensRaw != null && lastRefillRaw != null) {
            tokens = Double.parseDouble(String.valueOf(tokensRaw));
            long lastRefill = (long) Double.parseDouble(String.valueOf(lastRefillRaw));
            double elapsed = Math.max(0, nowMillis - lastRefill) / 1000.0;
            tokens = Math.min(capacity, tokens + elapsed * refillRate);
        }
        long remaining = (long) Math.floor(tokens);
        return RateLimitResult.builder()
                .allowed(tokens >= 1)
                .limit(config.getRequestLimit())
                .remaining(remaining)
                .resetEpochSeconds(Instant.now().getEpochSecond())
                .algorithm(getType())
                .build();
    }

    private double effectiveRefillRate(RateLimitConfig config) {
        if (config.getTokensPerSecond() > 0) {
            return config.getTokensPerSecond();
        }
        // Fall back to deriving a rate from the limit/window if not explicitly set.
        if (config.getWindowSizeSeconds() > 0) {
            return (double) config.getRequestLimit() / config.getWindowSizeSeconds();
        }
        return config.getRequestLimit();
    }

    /** {@inheritDoc} */
    @Override
    public AlgorithmType getType() {
        return AlgorithmType.TOKEN_BUCKET;
    }
}
