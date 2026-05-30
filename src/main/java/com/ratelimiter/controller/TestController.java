package com.ratelimiter.controller;

import com.ratelimiter.config.RateLimiterProperties;
import com.ratelimiter.dto.TestRequestResponse;
import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.service.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Test endpoint used to simulate a rate-limited request end-to-end.
 *
 * <p>The actual rate limiting decision is enforced by the servlet filter; this
 * controller simply reports the decision attached to the request and is reached
 * only when the request was allowed.</p>
 */
@RestController
@RequestMapping("/api/v1/test")
@RequiredArgsConstructor
@Tag(name = "Test", description = "Simulate rate-limited traffic")
public class TestController {

    /** Request attribute populated by the rate limit filter. */
    public static final String RESULT_ATTRIBUTE = "rateLimitResult";

    private final RateLimiterProperties properties;
    private final RateLimitService rateLimitService;

    /**
     * Simulates a single request. Reaching this method means the filter allowed it.
     */
    @Operation(summary = "Simulate a single request subject to rate limiting")
    @PostMapping("/request")
    public ResponseEntity<TestRequestResponse> simulate(HttpServletRequest request) {
        String merchantId = request.getHeader(properties.getMerchantHeader());
        RateLimitResult result = (RateLimitResult) request.getAttribute(RESULT_ATTRIBUTE);
        if (result == null) {
            // Filter excluded this path or attribute missing; evaluate directly.
            result = rateLimitService.check(merchantId);
        }
        TestRequestResponse body = TestRequestResponse.builder()
                .merchantId(merchantId)
                .allowed(result.isAllowed())
                .algorithm(result.getAlgorithm())
                .limit(result.getLimit())
                .remaining(result.getRemaining())
                .resetEpochSeconds(result.getResetEpochSeconds())
                .message("Request allowed")
                .build();
        return ResponseEntity.status(HttpStatus.OK).body(body);
    }
}
