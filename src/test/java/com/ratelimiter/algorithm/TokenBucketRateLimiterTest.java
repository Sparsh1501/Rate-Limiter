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
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TokenBucketRateLimiter} using a mocked Redis template.
 */
@ExtendWith(MockitoExtension.class)
class TokenBucketRateLimiterTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    @SuppressWarnings("rawtypes")
    private RedisScript script;

    private TokenBucketRateLimiter limiter;
    private RateLimitConfig config;

    @BeforeEach
    void setUp() {
        RateLimiterMetrics metrics = new RateLimiterMetrics(new SimpleMeterRegistry());
        limiter = new TokenBucketRateLimiter(redisTemplate, metrics, script);
        config = RateLimitConfig.builder()
                .merchantId("m1")
                .algorithm(AlgorithmType.TOKEN_BUCKET)
                .requestLimit(10)
                .windowSizeSeconds(1)
                .tokensPerSecond(5)
                .build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void allowsWhenTokensAvailable() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(List.of(1L, 9L, 1000L, 10L));

        RateLimitResult result = limiter.evaluate("m1", config);

        assertThat(result.isAllowed()).isTrue();
        assertThat(result.getRemaining()).isEqualTo(9);
        assertThat(result.getAlgorithm()).isEqualTo(AlgorithmType.TOKEN_BUCKET);
    }

    @Test
    @SuppressWarnings("unchecked")
    void blocksWhenNoTokens() {
        when(redisTemplate.execute(any(RedisScript.class), anyList(), anyString(), anyString(), anyString(), anyString()))
                .thenReturn(List.of(0L, 0L, 1005L, 10L));

        assertThat(limiter.evaluate("m1", config).isAllowed()).isFalse();
    }
}
