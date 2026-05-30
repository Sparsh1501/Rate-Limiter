package com.ratelimiter.algorithm;

import com.ratelimiter.model.AlgorithmType;
import com.ratelimiter.model.RateLimitConfig;
import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.service.RateLimiterMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link FixedWindowRateLimiter} using a mocked Redis template.
 */
@ExtendWith(MockitoExtension.class)
class FixedWindowRateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    @SuppressWarnings("rawtypes")
    private RedisScript script;

    private RateLimiterMetrics metrics;
    private FixedWindowRateLimiter limiter;
    private RateLimitConfig config;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        metrics = new RateLimiterMetrics(new io.micrometer.core.instrument.simple.SimpleMeterRegistry());
        limiter = new FixedWindowRateLimiter(redisTemplate, metrics, script);
        config = RateLimitConfig.builder()
                .merchantId("m1")
                .algorithm(AlgorithmType.FIXED_WINDOW)
                .requestLimit(5)
                .windowSizeSeconds(60)
                .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void allowsRequestWhenUnderLimit() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString(), anyString()))
                .thenReturn(List.of(1L, 4L, 1000L, 5L));

        RateLimitResult result = limiter.evaluate("m1", config);

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getRemaining()).isEqualTo(4);
        assertThat(result.getLimit()).isEqualTo(5);
        assertThat(result.getAlgorithm()).isEqualTo(AlgorithmType.FIXED_WINDOW);
    }

    @Test
    @SuppressWarnings("unchecked")
    void blocksRequestWhenOverLimit() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString(), anyString()))
                .thenReturn(List.of(0L, 0L, 1000L, 5L));

        RateLimitResult result = limiter.evaluate("m1", config);

        assertThat(result.isAllowed()).isFalse();
        assertThat(result.getRemaining()).isZero();
    }

    @Test
    @SuppressWarnings("unchecked")
    void failsOpenWhenRedisUnavailable() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString(), anyString()))
                .thenThrow(new QueryTimeoutException("redis down"));

        RateLimitResult result = limiter.evaluate("m1", config);

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getLimit()).isEqualTo(5);
    }
}
