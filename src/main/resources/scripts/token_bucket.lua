-- Token Bucket rate limiting (atomic).
-- KEYS[1] = bucket hash { tokens, lastRefill }
-- ARGV[1] = bucket capacity
-- ARGV[2] = refill rate (tokens per second)
-- ARGV[3] = current time in milliseconds
-- ARGV[4] = tokens requested (normally 1)
-- Returns: { allowed(1/0), remaining, resetEpochSeconds, capacity }

local capacity = tonumber(ARGV[1])
local refillRate = tonumber(ARGV[2])
local now = tonumber(ARGV[3])
local requested = tonumber(ARGV[4])

local data = redis.call('HMGET', KEYS[1], 'tokens', 'lastRefill')
local tokens = tonumber(data[1])
local lastRefill = tonumber(data[2])

if tokens == nil then
    tokens = capacity
    lastRefill = now
end

-- Refill based on elapsed wall-clock time.
local elapsedSeconds = math.max(0, now - lastRefill) / 1000.0
tokens = math.min(capacity, tokens + (elapsedSeconds * refillRate))

local allowed = 0
if tokens >= requested then
    tokens = tokens - requested
    allowed = 1
end

redis.call('HSET', KEYS[1], 'tokens', tokens, 'lastRefill', now)

-- Expire idle buckets after they would have fully refilled.
if refillRate > 0 then
    redis.call('EXPIRE', KEYS[1], math.ceil(capacity / refillRate) + 1)
end

local remaining = math.floor(tokens)
local reset = math.floor(now / 1000)
if allowed == 0 and refillRate > 0 then
    local needed = requested - tokens
    reset = math.floor(now / 1000) + math.ceil(needed / refillRate)
end

return { allowed, remaining, reset, capacity }
