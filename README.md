<h1 align="center">QTSurfer API Client · Java</h1>

<p align="center">
  <a href="https://github.com/QTSurfer/api-client-java/actions/workflows/ci.yml"><img src="https://github.com/QTSurfer/api-client-java/actions/workflows/ci.yml/badge.svg" alt="CI"></a>
  <a href="https://jitpack.io/#com.qtsurfer/api-client"><img src="https://jitpack.io/v/com.qtsurfer/api-client.svg" alt="JitPack"></a>
  <img src="https://img.shields.io/badge/JDK-17%2B-blue?logo=openjdk&logoColor=white" alt="JDK 17+">
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License"></a>
</p>

<p align="center">
  Auto-generated Java client for the <a href="https://github.com/QTSurfer/qtsurfer-api">QTSurfer API</a>, built from the OpenAPI 3.1 spec with <a href="https://openapi-generator.tech">openapi-generator</a> and the JDK's <code>java.net.http.HttpClient</code>.
</p>

<p align="center">
  <code>com.qtsurfer:api-client</code> · <code>com.qtsurfer:api-client</code>
</p>

---

Intentionally thin: one method per endpoint, 1:1 with the spec. For workflow orchestration (polling, retries, domain objects, unified errors), use [`com.qtsurfer:sdk`](https://github.com/QTSurfer/sdk-java).

- **Zero HTTP runtime deps** — `java.net.http.HttpClient` (JDK built-in) + Jackson for JSON.
- **Spec-driven** — generated sources fetched from [`QTSurfer/qtsurfer-api`](https://github.com/QTSurfer/qtsurfer-api) on every build.
- **JDK 17+** — modern language features, long-term support.
- **Distributed via JitPack** today; Maven Central later.

## Installation

### Via JitPack

Add the JitPack repository and the dependency:

```xml
<repositories>
  <repository>
    <id>jitpack.io</id>
    <url>https://jitpack.io</url>
  </repository>
</repositories>

<dependency>
  <groupId>com.qtsurfer</groupId>
  <artifactId>api-client-java</artifactId>
  <version>0.3.0</version>
</dependency>
```

For Gradle:

```gradle
repositories { maven { url 'https://jitpack.io' } }
dependencies { implementation 'com.qtsurfer:api-client:0.3.0' }
```

### Via Maven Central (future)

Once published to Central, the coordinate will be `com.qtsurfer:api-client:0.3.0`.

## Quick start

```java
import com.qtsurfer.api.client.api.ExchangeApi;
import com.qtsurfer.api.client.invoker.ApiClient;
import com.qtsurfer.api.client.model.Exchange;

import java.util.List;

ApiClient client = new ApiClient();
client.updateBaseUri("https://api.qtsurfer.com/v1");
client.setRequestInterceptor(builder ->
    builder.header("Authorization", "Bearer " + System.getenv("QTSURFER_TOKEN")));

ExchangeApi exchanges = new ExchangeApi(client);
List<Exchange> result = exchanges.getExchanges();
```

### API key → JWT

Every endpoint above expects a short-lived JWT in `Authorization: Bearer …`.
Exchange a long-lived API key for one via `AuthApi.auth()`:

```java
import com.qtsurfer.api.client.api.AuthApi;
import com.qtsurfer.api.client.invoker.ApiClient;
import com.qtsurfer.api.client.model.AuthTokenResponse;

ApiClient apikeyClient = new ApiClient();
apikeyClient.updateBaseUri("https://api.qtsurfer.com/v1");
apikeyClient.setRequestInterceptor(builder ->
    builder.header("X-API-Key", System.getenv("QTSURFER_APIKEY")));

AuthTokenResponse token = new AuthApi(apikeyClient).auth();
String jwt = token.getAccessToken();  // feed to a Bearer-authed ApiClient
```

For production use, prefer the [`com.qtsurfer:sdk`](https://github.com/QTSurfer/sdk-java)
`auth(apikey)` helper — it returns an `AuthenticatedClient` that refreshes the
JWT transparently, reads `QTSURFER_APIKEY` from the environment, and supports
pluggable token stores so callers don't reinvent that plumbing.

## API surface

| API class | Methods |
| --- | --- |
| `AuthApi` | `auth()` — exchange API key for a short-lived JWT |
| `ExchangeApi` | `getExchanges()`, `getInstruments(exchangeId)` |
| `ExchangeBinaryDownloads` | `getTickersHour(...)`, `getKlinesHour(...)` — Lastra/Parquet streams (manual; see note below) |
| `StrategyApi` | `postStrategy(body, xCompileAsync)`, `getStrategyStatus(strategyId)` |
| `BacktestingApi` | `prepareBacktesting`, `getPreparationStatus`, `executeBacktesting`, `cancelExecution`, `getExecutionResult` |

All generated model types (`Exchange`, `InstrumentDetail`, `JobState`, `BacktestJobResult`, `ResultMap`, `ResponseError`, …) live under `com.qtsurfer.api.client.model`.

### Binary downloads (`/exchange/{ex}/tickers|klines/{base}/{quote}`)

These endpoints return raw [Lastra](https://github.com/QTSurfer/lastra-java) bytes (default) or Parquet (`format=parquet`). The auto-generated `ExchangeApi.getExchangeTickersHour` / `getExchangeKlinesHour` methods are unusable for binary payloads — openapi-generator's `native` library decodes the body as UTF-8 and feeds it to Jackson, which corrupts the bytes. Use `ExchangeBinaryDownloads` instead:

```java
import com.qtsurfer.api.client.binary.ExchangeBinaryDownloads;
import com.qtsurfer.api.client.binary.ExchangeBinaryDownloads.Format;

ExchangeBinaryDownloads downloads = new ExchangeBinaryDownloads(client);
try (var in = downloads.getTickersHour("binance", "BTC", "USDT", "2026-01-15T10")) {
    Files.copy(in, Path.of("BTC_USDT_2026-01-15_h10.lastra"));
}

try (var in = downloads.getKlinesHour("binance", "BTC", "USDT", "2026-01-15T10", Format.PARQUET)) {
    // feed into Apache Parquet, DuckDB, etc.
}
```

The class reuses the `ApiClient`'s `HttpClient` and request interceptor, so any `Authorization` header set at the client level applies automatically.

## Configuring the client

`ApiClient` exposes the underlying `HttpClient.Builder` and an `ObjectMapper`, plus hooks for request/response interceptors.

```java
client.updateBaseUri("https://api.qtsurfer.com/v1");

client.setRequestInterceptor(builder ->
    builder.header("Authorization", "Bearer " + token)
           .header("X-Request-Id", UUID.randomUUID().toString()));

client.setResponseInterceptor(response ->
    log.debug("HTTP {} {}", response.statusCode(), response.uri()));
```

## Regenerating the client

Generated sources are produced by the `openapi-generator-maven-plugin` during the `generate-sources` phase and compiled from `target/generated-sources/openapi`. To regenerate:

```bash
mvn -B clean generate-sources
```

The input spec URL is configured in `pom.xml` (`openapi.spec.url` property). Point it to a tag or commit for reproducible builds.

## Development

| Command | Description |
| --- | --- |
| `mvn verify` | Fetch spec, generate, compile, run tests, build jar + sources + javadoc |
| `mvn clean` | Remove `target/` |

## License

Apache-2.0 — see [LICENSE](./LICENSE).
