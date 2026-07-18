# Changelog

All notable changes to `com.qtsurfer:api-client` are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.6.0] — 2026-07-18

### Changed 🔄

- Regenerated against OpenAPI spec `0.99.1`, which renames 16 operationIds — no request/response shape, field, or endpoint changes. Generated method names follow the operationId 1:1, so every renamed operation gets a new method name:

  | Old method | New method |
  | --- | --- |
  | `AuthApi.auth()` | `AuthApi.authenticate()` |
  | `ExchangeApi.getExchanges()` | `ExchangeApi.listExchanges()` |
  | `ExchangeApi.getInstruments()` | `ExchangeApi.listInstruments()` |
  | `ExchangeApi.getSegmentInstruments()` | `ExchangeApi.listSegmentInstruments()` |
  | `ExchangeApi.getExchangeTickersHour()` | `ExchangeApi.downloadTickers()` |
  | `ExchangeApi.getExchangeKlinesHour()` | `ExchangeApi.downloadKlines()` |
  | `StrategyApi.postStrategy()` | `StrategyApi.compileStrategy()` |
  | `StrategyApi.getStrategyStatus()` | `StrategyApi.getStrategy()` |
  | `BacktestingApi.prepareBacktesting()` | `BacktestingApi.prepareBacktest()` |
  | `BacktestingApi.getPreparationStatus()` | `BacktestingApi.getPrepareStatus()` |
  | `BacktestingApi.executeSweepBacktesting()` | `BacktestingApi.executeSweep()` |
  | `BacktestingApi.getExecuteSweepResult()` | `BacktestingApi.getSweepResult()` |
  | `BacktestingApi.cancelExecuteSweep()` | `BacktestingApi.cancelSweep()` |
  | `BacktestingApi.executeBacktesting()` | `BacktestingApi.executeBacktest()` |
  | `BacktestingApi.cancelExecution()` | `BacktestingApi.cancelBacktest()` |
  | `BacktestingApi.getExecutionResult()` | `BacktestingApi.getBacktestResult()` |

  The `BacktestingApi` sweep contract (`executeSweep` / `getSweepResult` / `cancelSweep`) is now documented in the README's API surface table — it was generated previously too (under the old names) but had not been called out there.

- As a byproduct of the operationId renames, openapi-generator's synthesized names for inline (non-`$ref`) request/response schemas also changed, since those names are derived from the operationId. Field-for-field these types are unchanged from the prior version — only the class name moved:
  - `CancelExecution200Response` → `CancelBacktest200Response`
  - `GetStrategyStatus200Response` → `GetStrategy200Response`
  - `PostStrategy200Response` → `CompileStrategy200Response`
  - `PrepareBacktestingRequest` → `PrepareRequest`
  - `ExecuteBacktestingRequest` → `ExecuteBacktestRequest`

## [0.5.0] — 2026-07-11

### Changed 🔄

- `BacktestingApi.getPreparationStatus(exchangeId, type, jobId)` now returns `PrepareJobState` instead of `JobState`, matching OpenAPI spec 0.98.0. `PrepareJobState` is generated as a standalone class (it does not extend `JobState`) that duplicates the `JobState` fields (`contextId`, `status`, `statusDetail`, `size`, `completed`, `startTime`, `endTime`, `dataFrom`, `dataTo`) and adds `coverageRatio` (`hoursWithData / totalHours` in `[0,1]`), `totalHours`, `hoursWithData`, and `hoursWithoutData` — a `List<PrepareJobStateAllOfHoursWithoutData>`, each with `hour`, `expected`, and a `rationale` enum (`PENDING_CONVERSION`, `LOW_ACTIVITY`, `UNKNOWN`).
- `JobState.StatusEnum` no longer has a `PARTIAL` value. Remaining values: `NEW`, `STARTED`, `COMPLETED`, `ABORTED`, `FAILED`.

### Added ✨

- `PrepareJobState` and `PrepareJobStateAllOfHoursWithoutData` generated models, carrying the per-hour data-coverage summary returned by `getPreparationStatus` for single-instrument prepare jobs.

## [0.4.0] — 2026-07-10

### Changed 🔄

- `ExchangeApi.getInstruments(exchangeId)` now returns an `InstrumentListResponse` HAL envelope (`data`, `meta`, `links`) instead of a bare `List<InstrumentDetail>`, matching OpenAPI spec 0.97.0. `data` still carries the `List<InstrumentDetail>`, `meta` carries `updatedAt`/`exchange`/`segment` (`InstrumentListMeta`), and `links` carries `self`/`spot`/`futures` HAL links (`InstrumentLinks`, `HalLink`).
- `InstrumentDetail` no longer has flat `dataFrom`/`dataTo` fields. Data availability now lives under `coverage` (`InstrumentCoverage`), with a `CoverageWindow` (`from`, `to`, optional `inactiveSince`) per data type — `tickers` and `klines`.

### Added ✨

- `ExchangeApi.getSegmentInstruments(exchangeId, segment)` — `GET /exchange/{exchangeId}/{segment}/instruments`, returning the same `InstrumentListResponse` envelope scoped to `spot` or `futures`.

### Fixed 🐛

- Bumped `openapi-generator-maven-plugin` `7.11.0` → `7.14.0`. On 7.11.0 the OpenAPI 3.1 parser inlined the `$ref` array items of `InstrumentListResponse.data`, so the generator synthesized a duplicate `InstrumentListResponseDataInner` model and `getData()` returned `List<InstrumentListResponseDataInner>` instead of `List<InstrumentDetail>`. 7.14.0 resolves the `$ref` correctly, so the envelope reuses the canonical `InstrumentDetail` / `InstrumentCoverage` / `CoverageWindow` / `HalLink` models.

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
