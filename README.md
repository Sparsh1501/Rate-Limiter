# Distributed Rate Limiter Service

> Redis-backed, multi-algorithm, per-merchant rate limiting for your API gateway — atomic under load, fail-open under failure.

[![CI](https://github.com/Sparsh1501/Rate-Limiter/actions/workflows/ci.yml/badge.svg)](https://github.com/Sparsh1501/Rate-Limiter/actions/workflows/ci.yml)
[![Java](https://img.shields.io/badge/Java-17-orange?logo=openjdk&logoColor=white)](https://adoptium.net/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.2-6DB33F?logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)](https://redis.io/)
[![Gradle](https://img.shields.io/badge/Gradle-8.7-02303A?logo=gradle&logoColor=white)](https://gradle.org/)
[![Docker](https://img.shields.io/badge/Docker-Compose-2496ED?logo=docker&logoColor=white)](https://docs.docker.com/compose/)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://www.apache.org/licenses/LICENSE-2.0)

A production-ready, standalone rate limiting microservice built with **Java 17 + Spring Boot 3**, backed by **Redis 7**. It supports three algorithms with per-merchant configuration and is designed to sit in front of an API gateway.

## Features

- **Three algorithms** — Fixed Window, Sliding Window Log, Token Bucket. Each implements a common `RateLimiterAlgorithm` interface.
- **Atomic Redis operations** — every read-modify-write is performed by a server-side **Lua script** (`RedisScript`) so the limiter is correct under high concurrency.
- **Per-merchant configuration** — stored as Redis hashes, managed through CRUD REST endpoints, with a default fallback config.
- **Servlet filter enforcement** — a `OncePerRequestFilter` reads `X-Merchant-ID`, runs the merchant's algorithm and returns `429` with `X-RateLimit-Limit`, `X-RateLimit-Remaining`, and `X-RateLimit-Reset` headers.
- **Fail-open** — if Redis is unreachable, requests are allowed (configurable strategy baked into the algorithm base class).
- **Observability** — Micrometer metrics exported to Prometheus, with a provisioned Grafana dashboard (RPS, blocked requests, Redis latency, cache hit ratio).
- **OpenAPI** — interactive docs at `/swagger-ui.html`.

## Tech Stack

Java 17 · Spring Boot 3.2 · Spring Data Redis (Lettuce) · Spring AOP · Gradle · Docker Compose · JUnit 5 + Testcontainers · Micrometer/Prometheus/Grafana · springdoc OpenAPI.

## Project Layout

```
src/main/java/com/ratelimiter/
├── algorithm/   # RateLimiterAlgorithm + Fixed/Sliding/TokenBucket
├── config/      # RedisConfig (+ Lua scripts), OpenApiConfig, properties
├── controller/  # MerchantConfigController, TestController
├── dto/         # Request/Response/Error DTOs
├── exception/   # Custom exceptions + GlobalExceptionHandler
├── filter/      # RateLimitFilter (OncePerRequestFilter)
├── model/       # AlgorithmType, RateLimitConfig, RateLimitResult
├── repository/  # RateLimitConfigRepository
├── service/     # RateLimitService, MerchantConfigService, metrics, registry
└── util/        # RedisKeys
src/main/resources/scripts/  # fixed_window.lua, sliding_window.lua, token_bucket.lua
```

## Redis Key Design

```
ratelimiter:merchant:{merchantId}:config          # Merchant config hash
ratelimiter:merchant:{merchantId}:fixedwindow     # Fixed window counter (TTL)
ratelimiter:merchant:{merchantId}:slidingwindow   # Sorted set of timestamps
ratelimiter:merchant:{merchantId}:tokenbucket     # Token bucket hash {tokens, lastRefill}
```

## API

| Method | Path | Description |
| ------ | ---- | ----------- |
| POST | `/api/v1/merchants/{merchantId}/config` | Create/update config |
| GET | `/api/v1/merchants/{merchantId}/config` | Get config |
| DELETE | `/api/v1/merchants/{merchantId}/config` | Delete config |
| GET | `/api/v1/merchants/{merchantId}/status` | Current usage stats |
| POST | `/api/v1/test/request` | Simulate a rate-limited request (send `X-Merchant-ID`) |
| GET | `/actuator/prometheus` | Metrics scrape endpoint |

## Build & Test

```bash
./gradlew build          # compile + unit tests (Testcontainers ITs need Docker)
./gradlew test           # run all tests
./gradlew bootJar        # produce the runnable jar
```

> Integration tests (`*IT`) use Testcontainers and require a running Docker daemon.

## Run with Docker Compose

```bash
docker-compose up --build
```

Starts four containers:

| Service | URL |
| ------- | --- |
| app | http://localhost:8080 (Swagger: `/swagger-ui.html`) |
| redis | localhost:6379 |
| prometheus | http://localhost:9090 |
| grafana | http://localhost:3000 (admin/admin) |

## Quick Demo

Create a merchant config with a limit of 5, then hit the test endpoint 10 times:

```bash
# 1. Create config (Fixed Window, 5 requests / 60s)
curl -X POST http://localhost:8080/api/v1/merchants/acme/config \
  -H "Content-Type: application/json" \
  -d '{"algorithm":"FIXED_WINDOW","requestLimit":5,"windowSizeSeconds":60,"tokensPerSecond":0}'

# 2. Fire 10 rapid requests — first 5 return 200, the rest 429
for i in $(seq 1 10); do
  curl -s -o /dev/null -w "request %{http_code}\n" \
    -X POST http://localhost:8080/api/v1/test/request \
    -H "X-Merchant-ID: acme"
done
```

Expected output:

```
request 200
request 200
request 200
request 200
request 200
request 429
request 429
request 429
request 429
request 429
```

PowerShell equivalent for the loop:

```powershell
1..10 | ForEach-Object {
  try {
    $r = Invoke-WebRequest -Method Post -Uri http://localhost:8080/api/v1/test/request -Headers @{ 'X-Merchant-ID' = 'acme' }
    "request $($r.StatusCode)"
  } catch { "request $($_.Exception.Response.StatusCode.value__)" }
}
```

## Configuration

All settings bind to `RateLimiterProperties` (`rate-limiter.*` in `application.yml`):

```yaml
rate-limiter:
  default-limit: 100
  default-window-seconds: 60
  default-algorithm: SLIDING_WINDOW
  default-tokens-per-second: 10
  merchant-header: X-Merchant-ID
  config-cache-ttl-seconds: 10
```
