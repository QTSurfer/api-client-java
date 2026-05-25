# Changelog

All notable changes to `com.qtsurfer:api-client` are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.3.1] — 2026-05-25

### Added

- `AuthApi.auth()` — exchange an API key for a short-lived JWT against `POST /auth/token` (OpenAPI spec 0.95.1). The client now ships `AuthTokenResponse` and `AuthTokenError` models and an `apiKeyAuth` (`X-API-Key`) security scheme.
- `EquityPoint` model and `ResultMap.pnlTotalPercent` / `ResultMap.equityCurve` fields, carried forward from a prior spec bump that had not been regenerated here.

### Fixed

- README quickstart now reads the JWT from `QTSURFER_TOKEN` (was `JWT_API_TOKEN`), matching the TS and Python clients.

## [0.2.0] — 2026-05-17

### Changed

- Maven coordinates migrated to `com.qtsurfer:api-client` via JitPack custom domain (`git.qtsurfer.com`). Consumers should replace `com.github.QTSurfer:api-client-java:v0.1.x` with `com.qtsurfer:api-client:0.2.0`.
- Java packages renamed from `net.qtsurfer.api.client` to `com.qtsurfer.api.client` throughout.
- Tags no longer use the `v` prefix (e.g. `0.2.0` instead of `v0.2.0`); CI release workflow updated accordingly.

## [0.1.1] — 2026-04-15

### Fixed

- Downgrade Maven plugin versions (compiler 3.11.0, surefire 3.2.5, source 3.3.0, javadoc 3.6.3) so that the build works under JitPack's bundled Maven 3.5.x.

## [0.1.0] — 2026-04-15

### Added

- Initial release of the auto-generated Java API client, produced by [`openapi-generator`](https://openapi-generator.tech) against the [QTSurfer OpenAPI 3.1 spec](https://github.com/QTSurfer/qtsurfer-api).
- HTTP layer: `java.net.http.HttpClient` (JDK 17+, zero HTTP runtime dependencies).
- JSON: Jackson 2.18.
- Operations: `getExchanges`, `getInstruments`, `postStrategy`, `getStrategyStatus`, `prepareBacktesting`, `getPreparationStatus`, `executeBacktesting`, `cancelExecution`, `getExecutionResult`.
- Distribution via [JitPack](https://jitpack.io/#QTSurfer/api-client-java).
