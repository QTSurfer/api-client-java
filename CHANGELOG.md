# Changelog

All notable changes to `net.qtsurfer:api-client` are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.1.0] — 2026-04-15

### Added

- Initial release of the auto-generated Java API client, produced by [`openapi-generator`](https://openapi-generator.tech) against the [QTSurfer OpenAPI 3.1 spec](https://github.com/QTSurfer/qtsurfer-api).
- HTTP layer: `java.net.http.HttpClient` (JDK 17+, zero HTTP runtime dependencies).
- JSON: Jackson 2.18.
- Operations: `getExchanges`, `getInstruments`, `postStrategy`, `getStrategyStatus`, `prepareBacktesting`, `getPreparationStatus`, `executeBacktesting`, `cancelExecution`, `getExecutionResult`.
- Distribution via [JitPack](https://jitpack.io/#QTSurfer/api-client-java).
