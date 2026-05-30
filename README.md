Longer description (for the top of the README or the "Description" section)
Distributed Rate Limiter Service is a standalone, Redis-backed microservice that provides API-gateway-grade rate limiting with pluggable algorithms and per-merchant configuration.

Highlights

Three algorithms behind a common RateLimiterAlgorithm interface — Fixed Window, Sliding Window Log, and Token Bucket.
Atomic by design — every read-modify-write runs as a server-side Redis Lua script, so limits hold correctly under high concurrency (verified with a 100-thread Testcontainers test).
Per-merchant configuration stored as Redis hashes, managed via CRUD REST endpoints, with a default fallback and a short-lived in-process cache.
Drop-in enforcement through a OncePerRequestFilter that reads X-Merchant-ID and returns 429 with X-RateLimit-Limit, X-RateLimit-Remaining, X-RateLimit-Reset, and Retry-After headers.
Resilient — fail-open strategy keeps traffic flowing if Redis is unreachable.
Observable — Micrometer metrics (requests, Redis latency, cache hit ratio) exported to Prometheus with a provisioned Grafana dashboard.
Documented & containerized — OpenAPI/Swagger UI, multi-stage Docker build, and a one-command docker-compose stack (app + Redis + Prometheus + Grafana).
Tech: Java 17 · Spring Boot 3 · Spring Data Redis (Lettuce) · Spring AOP · Gradle · Redis 7 · Docker Compose · JUnit 5 + Testcontainers · Micrometer/Prometheus/Grafana · springdoc OpenAPI.
