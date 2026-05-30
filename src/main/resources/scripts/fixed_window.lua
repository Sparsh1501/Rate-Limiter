-- Fixed Window rate limiting (atomic).
-- KEYS[1] = counter key
-- ARGV[1] = request limit
-- ARGV[2] = window size in seconds
-- ARGV[3] = current epoch seconds (caller clock)
-- Returns: { allowed(1/0), remaining, resetEpochSeconds, limit }

local limit = tonumber(ARGV[1])
local window = tonumber(ARGV[2])
local now = tonumber(ARGV[3])

local current = redis.call('INCR', KEYS[1])
local ttl = redis.call('TTL', KEYS[1])

-- First request in this window (or TTL was lost): (re)apply the expiry.
if current == 1 or ttl < 0 then
    redis.call('EXPIRE', KEYS[1], window)
    ttl = window
end

local allowed = 0
local remaining = 0
if current <= limit then
    allowed = 1
    remaining = limit - current
end

local reset = now + ttl
return { allowed, remaining, reset, limit }
