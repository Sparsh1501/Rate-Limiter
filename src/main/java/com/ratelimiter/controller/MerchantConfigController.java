package com.ratelimiter.controller;

import com.ratelimiter.dto.MerchantStatusResponse;
import com.ratelimiter.dto.RateLimitConfigRequest;
import com.ratelimiter.dto.RateLimitConfigResponse;
import com.ratelimiter.model.RateLimitConfig;
import com.ratelimiter.model.RateLimitResult;
import com.ratelimiter.service.MerchantConfigService;
import com.ratelimiter.service.RateLimitService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * CRUD and status endpoints for per-merchant rate limit configuration.
 *
 * <p>Controllers contain no business logic — they delegate entirely to the
 * service layer.</p>
 */
@RestController
@RequestMapping("/api/v1/merchants/{merchantId}")
@RequiredArgsConstructor
@Tag(name = "Merchant Configuration", description = "Manage per-merchant rate limit configuration")
public class MerchantConfigController {

    private final MerchantConfigService configService;
    private final RateLimitService rateLimitService;

    /**
     * Creates or updates a merchant's configuration.
     */
    @Operation(summary = "Create or update a merchant's rate limit configuration")
    @PostMapping("/config")
    public ResponseEntity<RateLimitConfigResponse> upsertConfig(@PathVariable String merchantId,
                                                                @Valid @RequestBody RateLimitConfigRequest request) {
        RateLimitConfig config = configService.saveConfig(merchantId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(RateLimitConfigResponse.from(config));
    }

    /**
     * Retrieves a merchant's configuration.
     */
    @Operation(summary = "Get a merchant's rate limit configuration")
    @GetMapping("/config")
    public ResponseEntity<RateLimitConfigResponse> getConfig(@PathVariable String merchantId) {
        RateLimitConfig config = configService.getConfig(merchantId);
        return ResponseEntity.ok(RateLimitConfigResponse.from(config));
    }

    /**
     * Deletes a merchant's configuration.
     */
    @Operation(summary = "Delete a merchant's rate limit configuration")
    @DeleteMapping("/config")
    public ResponseEntity<Void> deleteConfig(@PathVariable String merchantId) {
        configService.deleteConfig(merchantId);
        return ResponseEntity.noContent().build();
    }

    /**
     * Returns current usage stats for a merchant without consuming quota.
     */
    @Operation(summary = "Get current rate limit usage for a merchant")
    @GetMapping("/status")
    public ResponseEntity<MerchantStatusResponse> getStatus(@PathVariable String merchantId) {
        RateLimitResult result = rateLimitService.status(merchantId);
        MerchantStatusResponse response = MerchantStatusResponse.builder()
                .merchantId(merchantId)
                .algorithm(result.getAlgorithm())
                .limit(result.getLimit())
                .used(result.getLimit() - result.getRemaining())
                .remaining(result.getRemaining())
                .resetEpochSeconds(result.getResetEpochSeconds())
                .build();
        return ResponseEntity.ok(response);
    }
}
