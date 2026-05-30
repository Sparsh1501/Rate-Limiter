package com.ratelimiter.dto;

import com.ratelimiter.model.AlgorithmType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

/**
 * Request payload used to create or update a merchant's rate limit configuration.
 */
@Data
@Schema(description = "Rate limit configuration for a merchant")
public class RateLimitConfigRequest {

    @NotNull(message = "algorithm is required")
    @Schema(description = "Rate limiting algorithm", example = "SLIDING_WINDOW")
    private AlgorithmType algorithm;

    @Positive(message = "requestLimit must be positive")
    @Schema(description = "Maximum requests per window / bucket capacity", example = "100")
    private int requestLimit;

    @Positive(message = "windowSizeSeconds must be positive")
    @Schema(description = "Window size in seconds (window-based algorithms)", example = "60")
    private int windowSizeSeconds;

    @PositiveOrZero(message = "tokensPerSecond must be zero or positive")
    @Schema(description = "Refill rate for token bucket (tokens/second)", example = "10")
    private double tokensPerSecond;
}
