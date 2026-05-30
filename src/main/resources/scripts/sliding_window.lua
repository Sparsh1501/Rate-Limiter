-- Sliding Window Log rate limiting (atomic).
-- KEYS[1] = sorted set key
-- ARGV[1] = request limit
-- ARGV[2] = window size in seconds
-- ARGV[3] = current time in milliseconds
-- ARGV[4] = unique member id for this request
-- Returns: { allowed(1/0), remaining, resetEpochSeconds, limit }

local limit = tonumber(ARGV[1])
local windowMs = tonumber(ARGV[2]) * 1000
local now = tonumber(ARGV[3])
local member = ARGV[4]

-- Drop entries that fell out of the sliding window.
redis.call('ZREMRANGEBYSCORE', KEYS[1], 0, now - windowMs)

local count = redis.call('ZCARD', KEYS[1])
local allowed = 0
local remaining = 0

if count < limit then
    redis.call('ZADD', KEYS[1], now, member)
    count = count + 1
    allowed = 1
    remaining = limit - count
end

-- Keep the set from living forever once a merchant goes idle.
redis.call('PEXPIRE', KEYS[1], windowMs)

-- Reset = when the oldest in-window request expires.
local reset = math.floor((now + windowMs) / 1000)
local oldest = redis.call('ZRANGE', KEYS[1], 0, 0, 'WITHSCORES')
if oldest[2] then
    reset = math.floor((tonumber(oldest[2]) + windowMs) / 1000)
end

return { allowed, remaining, reset, limit }
