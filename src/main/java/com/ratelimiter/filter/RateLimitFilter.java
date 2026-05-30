package com.ratelimiter.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.ratelimiter.config.RateLimiterProperties;
import com.ratelimiter.controller.TestController;
import com.ratelimiter.dto.ErrorResponse;
import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.service.RateLimitService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.List;

/**
 * Servlet filter that enforces rate limiting on every inbound HTTP request.
 *
 * <p>The merchant is identified from a configurable header. Requests on excluded
 * paths (management, docs, config CRUD) are passed through. Allowed requests
 * receive {@code X-RateLimit-*} headers; blocked requests are short-circuited
 * with HTTP 429 and a structured error body.</p>
 */
@Slf4j
@Component
@Order(1)
public class RateLimitFilter extends OncePerRequestFilter {

    private static final String HEADER_LIMIT = "X-RateLimit-Limit";
    private static final String HEADER_REMAINING = "X-RateLimit-Remaining";
    private static final String HEADER_RESET = "X-RateLimit-Reset";

    private final RateLimitService rateLimitService;
    private final RateLimiterProperties properties;
    private final ObjectMapper objectMapper;

    /**
     * @param rateLimitService the evaluation service
     * @param properties       rate limiter configuration
     * @param objectMapper     Jackson mapper for error serialization
     */
    public RateLimitFilter(RateLimitService rateLimitService,
                           RateLimiterProperties properties,
                           ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    /** {@inheritDoc} */
    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String merchantId = request.getHeader(properties.getMerchantHeader());
        if (merchantId == null || merchantId.isBlank()) {
            // No merchant context — nothing to rate limit.
            filterChain.doFilter(request, response);
            return;
        }

        RateLimitResult result = rateLimitService.check(merchantId);
        applyHeaders(response, result);
        request.setAttribute(TestController.RESULT_ATTRIBUTE, result);

        if (result.isAllowed()) {
            filterChain.doFilter(request, response);
        } else {
            writeTooManyRequests(request, response, result);
        }
    }

    /** {@inheritDoc} */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        List<String> excluded = properties.getExcludedPaths();
        return excluded != null && excluded.stream().anyMatch(path::startsWith);
    }

    private void applyHeaders(HttpServletResponse response, RateLimitResult result) {
        response.setHeader(HEADER_LIMIT, String.valueOf(result.getLimit()));
        response.setHeader(HEADER_REMAINING, String.valueOf(Math.max(0, result.getRemaining())));
        response.setHeader(HEADER_RESET, String.valueOf(result.getResetEpochSeconds()));
    }

    private void writeTooManyRequests(HttpServletRequest request,
                                      HttpServletResponse response,
                                      RateLimitResult result) throws IOException {
        long retryAfter = Math.max(0, result.getResetEpochSeconds() - Instant.now().getEpochSecond());
        response.setHeader("Retry-After", String.valueOf(retryAfter));
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);

        ErrorResponse body = ErrorResponse.builder()
                .timestamp(Instant.now())
                .status(HttpStatus.TOO_MANY_REQUESTS.value())
                .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                .message("Rate limit exceeded for algorithm " + result.getAlgorithm())
                .path(request.getRequestURI())
                .build();
        objectMapper.writeValue(response.getWriter(), body);
    }
}
