package com.ratelimiter.algorithm;

import com.ratelimiter.model.AlgorithmType;
import com.ratelimiter.model.RateLimitConfig;
import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.service.RateLimiterMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link SlidingWindowRateLimiter} using a mocked Redis template.
 */
@ExtendWith(MockitoExtension.class)
class SlidingWindowRateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    @SuppressWarnings("rawtypes")
    private RedisScript script;

    private SlidingWindowRateLimiter limiter;
    private RateLimitConfig config;

    @BeforeEach
    void setUp() {
        RateLimiterMetrics metrics = new RateLimiterMetrics(new SimpleMeterRegistry());
        limiter = new SlidingWindowRateLimiter(redisTemplate, metrics, script);
        config = RateLimitConfig.builder()
                .merchantId("m1")
                .algorithm(AlgorithmType.SLIDING_WINDOW)
                .requestLimit(3)
                .windowSizeSeconds(10)
                .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void allowsWhenUnderLimit() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(List.of(1L, 2L, 1234L, 3L));

        RateLimitResult result = limiter.evaluate("m1", config);

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getRemaining()).isEqualTo(2);
    }

    @Test
    @SuppressWarnings("unchecked")
    void blocksWhenAtLimit() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(List.of(0L, 0L, 1234L, 3L));

        assertThat(limiter.evaluate("m1", config).isAllowed()).isFalse();
    }

    @Test
    @SuppressWarnings("unchecked")
    void failsOpenOnConnectionFailure() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString(), anyString(), anyString()))
                .thenThrow(new RedisConnectionFailureException("down"));

        assertThat(limiter.evaluate("m1", config).isAllowed()).isTrue();
    }
}
