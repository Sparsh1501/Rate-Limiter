package com.ratelimiter.integration;

import com.ratelimiter.dto.RateLimitConfigRequest;
import com.ratelimiter.model.AlgorithmType;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * End-to-end HTTP test verifying that the servlet filter returns 429 once a
 * merchant breaches their configured limit, including the {@code X-RateLimit-*}
 * headers.
 */
@Testcontainers
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RateLimitFilterIT {

    @Container
    @SuppressWarnings("resource")
    static final GenericContainer<?> REDIS = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    void returns429AfterLimitIsBreached() {
        String merchantId = "merchant-" + UUID.randomUUID();
        int limit = 5;

        RateLimitConfigRequest config = new RateLimitConfigRequest();
        config.setAlgorithm(AlgorithmType.FIXED_WINDOW);
        config.setRequestLimit(limit);
        config.setWindowSizeSeconds(60);
        restTemplate.postForEntity(url("/api/v1/merchants/" + merchantId + "/config"), config, String.class);

        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Merchant-ID", merchantId);
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        int allowed = 0;
        int blocked = 0;
        ResponseEntity<String> last = null;
        for (int i = 0; i < 10; i++) {
            last = restTemplate.exchange(url("/api/v1/test/request"), HttpMethod.POST, entity, String.class);
            if (last.getStatusCode() == HttpStatus.TOO_MANY_REQUESTS) {
                blocked++;
            } else if (last.getStatusCode().is2xxSuccessful()) {
                allowed++;
            }
        }

        assertThat(allowed).isEqualTo(limit);
        assertThat(blocked).isEqualTo(10 - limit);
        assertThat(last).isNotNull();
        assertThat(last.getHeaders().getFirst("X-RateLimit-Limit")).isEqualTo(String.valueOf(limit));
        assertThat(last.getHeaders().getFirst("X-RateLimit-Remaining")).isEqualTo("0");
    }

    private String url(String path) {
        return "http://localhost:" + port + path;
    }
}
