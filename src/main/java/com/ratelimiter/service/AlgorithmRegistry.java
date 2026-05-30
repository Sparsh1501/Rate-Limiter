package com.ratelimiter.service;

import com.ratelimiter.algorithm.RateLimiterAlgorithm;
import com.ratelimiter.model.AlgorithmType;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Resolves a {@link RateLimiterAlgorithm} implementation from an {@link AlgorithmType}.
 *
 * <p>All algorithm beans are injected by Spring and indexed by their declared type,
 * keeping the lookup open for extension without modifying the service layer.</p>
 */
@Component
public class AlgorithmRegistry {

    private final Map<AlgorithmType, RateLimiterAlgorithm> algorithms = new EnumMap<>(AlgorithmType.class);

    /**
     * @param algorithmBeans all discovered algorithm implementations
     */
    public AlgorithmRegistry(List<RateLimiterAlgorithm> algorithmBeans) {
        for (RateLimiterAlgorithm algorithm : algorithmBeans) {
            algorithms.put(algorithm.getType(), algorithm);
        }
    }

    /**
     * @param type the algorithm type
     * @return the matching implementation
     * @throws IllegalStateException if no implementation is registered
     */
    public RateLimiterAlgorithm get(AlgorithmType type) {
        RateLimiterAlgorithm algorithm = algorithms.get(type);
        if (algorithm == null) {
            throw new IllegalStateException("No implementation registered for algorithm: " + type);
        }
        return algorithm;
    }
}
