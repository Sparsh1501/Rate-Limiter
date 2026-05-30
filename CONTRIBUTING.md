# Contributing

Thanks for your interest in improving the Distributed Rate Limiter Service!

## Getting started

1. Fork the repository and create a feature branch from `main`.
2. Make sure you have **JDK 17** and **Docker** installed (Docker is required for the Testcontainers integration tests).
3. Build and test:

   ```bash
   ./gradlew build
   ```

## Guidelines

- **Code style** — keep the existing conventions: constructor injection, no business logic in controllers, `@ConfigurationProperties` over `@Value`, and Javadoc on public classes/methods.
- **Tests** — add unit tests for new logic (mock Redis) and, where behaviour spans Redis, an integration test using Testcontainers. All Redis read-modify-write operations must remain atomic (Lua).
- **Commits** — write clear, imperative commit messages (e.g. `Add weighted token bucket support`).
- **Lint/format** — respect `.editorconfig`; CI runs `./gradlew build` on every push and PR.

## Submitting changes

1. Ensure `./gradlew build` passes locally.
2. Open a pull request against `main` describing the change and the motivation.
3. Link any related issues.

## Reporting issues

Please include the algorithm in use, the relevant `RateLimitConfig`, reproduction steps, and logs (with `merchantId`) where possible.
