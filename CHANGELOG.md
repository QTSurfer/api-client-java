# Changelog

All notable changes to `com.qtsurfer:api-client` are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.8.0] — 2026-08-12

Regenerated against OpenAPI spec `0.106.0`, which adds walk-forward validation and sensitivity
analysis to sweeps. Purely additive: no existing field, method, or type was renamed or removed.

### Added ✨

- `BacktestingApi.executeSweep(...)` accepts an optional `walkForward` (`WalkForwardRequest`:
  `folds`, `inSamplePct`) on the request body. Submitting it runs the sweep as walk-forward
  validation instead of a flat parameter sweep. `ExecuteSweepAccepted` gains a matching optional
  `walkForward` (`WalkForwardAccepted`: `folds`, `inSamplePct`, `totalRuns`) confirming the fold
  plan the moment the sweep is accepted — before any fold has finished, so it's safe to branch on
  while polling for whether a sweep is walk-forward or not.
- `BacktestingApi.getSweepResult(...)` response (`ExecuteSweepResult`) gains an optional
  `walkForward` (`WalkForwardResult`: `folds`, `inSamplePct`, `completedFolds`, `paramDrift`,
  `results` — a `List<WalkForwardFold>`). When present, the leaderboard is one row per completed
  fold (that fold's out-of-sample winner, with `runIx` carrying the fold index rather than a grid
  position) instead of one row per parameter point. `ranking` is always `raw` and no plateau, DSR,
  or PBO figure is reported for a walk-forward sweep — the out-of-sample scores are already the
  honest number.
- New endpoint `BacktestingApi.getSweepSensitivity(exchangeId, type, requestId, sweepId, objective)`
  — `GET .../executeSweep/{requestId}/{sweepId}/sensitivity` — returns `SweepSensitivity`
  (`sweepId`, `status`, `objective`, `rowsAnalysed`, `marginals`: `List<SweepMarginal>`,
  `heatmaps`: `List<SweepHeatmap>`, `heatmapsTruncated`) or a `404` `ResponseError` if the sweep
  is unknown or expired. Aggregated from the sweep's stored rows — no re-run, works on a sweep
  still in flight.
- `ExecuteSweepResult` gains `pbo` and `pboSplits` — probability-of-backtest-overfitting figures
  computed over the sweep's stored rows, present only for a non-walk-forward sweep.
- `SweepRunRow` gains optional `plateauScore`, `neighbourCount`, `deflatedSharpe`.
- `SweepProgress` gains `stalledSeconds` and `etaSeconds` (both optional).

### Changed 🔄

- `BacktestingApi.getSweepResult(...)` gains a `ranking` query parameter (`plateau` | `raw`,
  default `plateau`). This changes the *default* ordering of the existing `ranked` view: it now
  sorts by plateau score (the objective of the worst run in a parameter point's neighbourhood)
  rather than the raw objective, so a spike that doesn't survive a small parameter move no longer
  ranks first by default. The `ranked` view's rows and the `order=natural` view are otherwise
  unchanged; pass `ranking=raw` to get the old ordering. `ExecuteSweepResult` gains a `ranking`
  field reporting which ordering was actually used.
- `SweepProgress` gains three new **required** fields: `failedShards`, `retrying`, `notStarted`.
  Any hand-written code that constructs a `SweepProgress` (none exists in this repo today) would
  need to supply them.

## [0.7.0] — 2026-08-06

### Changed 🔄

- `StrategyApi.compileStrategy(body)` is now always synchronous, matching OpenAPI spec `0.102.0`. The async compile path is gone entirely: there is no more `X-Compile-Async` header parameter and no `202`/job-polling branch. `200` returns `CompileStrategy200Response` (`{strategyId}`, unchanged shape), `400` is a compile error, and a new `429` means too many compilations in flight — retry later.
- `StrategyApi.getStrategy(strategyId)` now returns `StrategyState` instead of the old `GetStrategy200Response` (`{jobId, status: New|Started|Completed|Aborted|Failed, strategyId, statusDetail}`). `StrategyState` carries `strategyId`, `validation` (generated enum `NOT_VALIDATED` / `PENDING` / `PASSED` / `FAILED`, wire values `not_validated` / `pending` / `passed` / `failed`), `compiledAt`, `requiredSources` (`TICKER` / `K_LINE` / `FUNDING_RATE`), `validatedAt`, `detail`, `notices`, `noticesTruncated`, `dryRunIncomplete`, and `validationStalled`.
- `ResultMap` (returned by `BacktestJobResult.getResults()`) gains optional `notices` (`List<Notice>`) and `noticesTruncated`, the same diagnostics shape now also carried on `StrategyState`.

### Added ✨

- `StrategyApi.validateStrategy(strategyId)` — `POST /strategy/{strategyId}/validate`. Idempotent: `200` returns the already-recorded `StrategyState` verdict, `202` means a check was just queued (poll `getStrategy` until `validation` leaves `pending`), `404` if the strategy is not registered.
- `Notice` — the engine-diagnostic shape (`level`, `code`, `message`, `provenance`: `EXECUTE` / `COMPILE_DRY_RUN`) now carried by both `StrategyState.notices` and `ResultMap.notices`.

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
