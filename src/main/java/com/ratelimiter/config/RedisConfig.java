package com.ratelimiter.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scripting.support.ResourceScriptSource;

import java.util.List;

/**
 * Wires the Redis template and the Lua {@link RedisScript} beans used by the
 * rate limiting algorithms.
 *
 * <p>All algorithms execute server-side Lua so that the read-modify-write cycle
 * is atomic under concurrency. Each script returns a list of longs:
 * {@code [allowed, remaining, resetEpochSeconds, limit]}.</p>
 */
@Configuration
public class RedisConfig {

    /**
     * String-based template; all rate limiter values are stored as strings/hashes.
     *
     * @param connectionFactory the auto-configured Lettuce connection factory
     * @return a ready to use {@link StringRedisTemplate}
     */
    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory connectionFactory) {
        return new StringRedisTemplate(connectionFactory);
    }

    /**
     * @return the atomic Fixed Window script
     */
    @Bean
    public RedisScript<List> fixedWindowScript() {
        return loadScript("scripts/fixed_window.lua");
    }

    /**
     * @return the atomic Sliding Window Log script
     */
    @Bean
    public RedisScript<List> slidingWindowScript() {
        return loadScript("scripts/sliding_window.lua");
    }

    /**
     * @return the atomic Token Bucket script
     */
    @Bean
    public RedisScript<List> tokenBucketScript() {
        return loadScript("scripts/token_bucket.lua");
    }

    @SuppressWarnings("rawtypes")
    private RedisScript<List> loadScript(String path) {
        DefaultRedisScript<List> script = new DefaultRedisScript<>();
        script.setScriptSource(new ResourceScriptSource(new ClassPathResource(path)));
        script.setResultType(List.class);
        return script;
    }
}
