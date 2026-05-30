package com.ratelimiter.service;

import com.ratelimiter.model.AlgorithmType;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Thin wrapper around Micrometer exposing the rate limiter's domain metrics.
 *
 * <ul>
 *   <li>{@code rate_limiter.requests.total} — tagged by merchantId, algorithm, result</li>
 *   <li>{@code rate_limiter.redis.latency} — Redis operation duration timer</li>
 *   <li>{@code rate_limiter.config.cache.hits} — config cache hit/miss counter</li>
 * </ul>
 */
@Component
public class RateLimiterMetrics {

    private final MeterRegistry registry;

    /**
     * @param registry the Micrometer meter registry
     */
    public RateLimiterMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    /**
     * Records the outcome of a rate limit evaluation.
     *
     * @param merchantId the merchant id
     * @param algorithm  the algorithm used
     * @param allowed    whether the request was allowed
     */
    public void recordRequest(String merchantId, AlgorithmType algorithm, boolean allowed) {
        Counter.builder("rate_limiter.requests.total")
                .tag("merchantId", merchantId)
                .tag("algorithm", algorithm.name())
                .tag("result", allowed ? "allowed" : "blocked")
                .register(registry)
                .increment();
    }

    /**
     * Records the duration of a Redis operation.
     *
     * @param nanos elapsed time in nanoseconds
     */
    public void recordRedisLatency(long nanos) {
        Timer.builder("rate_limiter.redis.latency")
                .publishPercentiles(0.5, 0.95, 0.99)
                .register(registry)
                .record(nanos, TimeUnit.NANOSECONDS);
    }

    /**
     * Records a configuration cache hit or miss.
     *
     * @param hit {@code true} for a cache hit, {@code false} for a miss
     */
    public void recordConfigCache(boolean hit) {
        Counter.builder("rate_limiter.config.cache.hits")
                .tag("result", hit ? "hit" : "miss")
                .register(registry)
                .increment();
    }
}
