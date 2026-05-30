package com.ratelimiter.service;

import com.ratelimiter.config.RateLimiterProperties;
import com.ratelimiter.dto.RateLimitConfigRequest;
import com.ratelimiter.exception.InvalidConfigException;
import com.ratelimiter.exception.MerchantNotFoundException;
import com.ratelimiter.model.AlgorithmType;
import com.ratelimiter.model.RateLimitConfig;
import com.ratelimiter.repository.RateLimitConfigRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MerchantConfigService} validation, defaults and caching.
 */
@ExtendWith(MockitoExtension.class)
class MerchantConfigServiceTest {

    @Mock
    private RateLimitConfigRepository repository;

    private MerchantConfigService service;

    @BeforeEach
    void setUp() {
        RateLimiterProperties properties = new RateLimiterProperties();
        RateLimiterMetrics metrics = new RateLimiterMetrics(new SimpleMeterRegistry());
        service = new MerchantConfigService(repository, properties, metrics);
    }

    @Test
    void rejectsNonPositiveLimit() {
        RateLimitConfigRequest request = new RateLimitConfigRequest();
        request.setAlgorithm(AlgorithmType.FIXED_WINDOW);
        request.setRequestLimit(0);
        request.setWindowSizeSeconds(60);

        assertThatThrownBy(() -> service.saveConfig("m1", request))
                .isInstanceOf(InvalidConfigException.class);
    }

    @Test
    void rejectsTokenBucketWithoutRefillRate() {
        RateLimitConfigRequest request = new RateLimitConfigRequest();
        request.setAlgorithm(AlgorithmType.TOKEN_BUCKET);
        request.setRequestLimit(10);
        request.setTokensPerSecond(0);

        assertThatThrownBy(() -> service.saveConfig("m1", request))
                .isInstanceOf(InvalidConfigException.class);
    }

    @Test
    void getConfigThrowsWhenMissing() {
        when(repository.findById("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getConfig("ghost"))
                .isInstanceOf(MerchantNotFoundException.class);
    }

    @Test
    void resolveFallsBackToDefaultsWhenNotConfigured() {
        when(repository.findById("new")).thenReturn(Optional.empty());

        RateLimitConfig config = service.resolveEffectiveConfig("new");

        assertThat(config.getAlgorithm()).isEqualTo(AlgorithmType.SLIDING_WINDOW);
        assertThat(config.getRequestLimit()).isEqualTo(100);
    }

    @Test
    void resolveUsesCacheOnSecondCall() {
        RateLimitConfig stored = RateLimitConfig.builder()
                .merchantId("m1")
                .algorithm(AlgorithmType.FIXED_WINDOW)
                .requestLimit(50)
                .windowSizeSeconds(30)
                .build();
        when(repository.findById("m1")).thenReturn(Optional.of(stored));

        service.resolveEffectiveConfig("m1");
        service.resolveEffectiveConfig("m1");

        // Second call should be served from cache, so repository hit only once.
        verify(repository, times(1)).findById("m1");
    }
}
