package com.ratelimiter.integration;

import com.ratelimiter.dto.RateLimitConfigRequest;
import com.ratelimiter.model.AlgorithmType;
import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.service.MerchantConfigService;
import com.ratelimiter.service.RateLimitService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Verifies atomicity of each algorithm under heavy concurrency against real Redis.
 *
 * <p>Fires 100 simultaneous requests against a freshly configured merchant and
 * asserts that exactly the configured limit are allowed and the rest blocked.</p>
 */
class RateLimiterConcurrencyIT extends AbstractRedisIntegrationTest {

    private static final int THREADS = 100;

    @Autowired
    private RateLimitService rateLimitService;

    @Autowired
    private MerchantConfigService configService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Test
    void fixedWindowAllowsExactlyTheLimitUnderConcurrency() throws InterruptedException {
        int limit = 20;
        String merchantId = configure(AlgorithmType.FIXED_WINDOW, limit, 60, 0);

        int allowed = fireConcurrently(merchantId);

        assertThat(allowed).isEqualTo(limit);
    }

    @Test
    void slidingWindowAllowsExactlyTheLimitUnderConcurrency() throws InterruptedException {
        int limit = 30;
        String merchantId = configure(AlgorithmType.SLIDING_WINDOW, limit, 60, 0);

        int allowed = fireConcurrently(merchantId);

        assertThat(allowed).isEqualTo(limit);
    }

    @Test
    void tokenBucketNeverAllowsMoreThanCapacityInstantaneously() throws InterruptedException {
        int capacity = 25;
        String merchantId = configure(AlgorithmType.TOKEN_BUCKET, capacity, 1, 1);

        int allowed = fireConcurrently(merchantId);

        // With a refill of 1 token/sec, a burst can only consume the initial capacity
        // (plus at most a negligible amount of refill during the burst window).
        assertThat(allowed).isGreaterThanOrEqualTo(capacity);
        assertThat(allowed).isLessThanOrEqualTo(capacity + 2);
    }

    private String configure(AlgorithmType algorithm, int limit, int window, double tokensPerSecond) {
        String merchantId = "merchant-" + UUID.randomUUID();
        RateLimitConfigRequest request = new RateLimitConfigRequest();
        request.setAlgorithm(algorithm);
        request.setRequestLimit(limit);
        request.setWindowSizeSeconds(window);
        request.setTokensPerSecond(tokensPerSecond);
        configService.saveConfig(merchantId, request);
        return merchantId;
    }

    private int fireConcurrently(String merchantId) throws InterruptedException {
        ExecutorService executor = Executors.newFixedThreadPool(THREADS);
        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(THREADS);
        AtomicInteger allowed = new AtomicInteger();

        for (int i = 0; i < THREADS; i++) {
            executor.submit(() -> {
                ready.countDown();
                try {
                    start.await();
                    RateLimitResult result = rateLimitService.check(merchantId);
                    if (result.isAllowed()) {
                        allowed.incrementAndGet();
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }

        ready.await(5, TimeUnit.SECONDS);
        start.countDown();
        done.await(15, TimeUnit.SECONDS);
        executor.shutdownNow();
        return allowed.get();
    }
}
