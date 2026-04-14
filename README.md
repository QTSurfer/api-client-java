# net.qtsurfer:api-client

<p align="center">
  <a href="https://github.com/QTSurfer/api-client-java/actions/workflows/ci.yml"><img src="https://github.com/QTSurfer/api-client-java/actions/workflows/ci.yml/badge.svg" alt="CI"></a>
  <a href="https://jitpack.io/#QTSurfer/api-client-java"><img src="https://jitpack.io/v/QTSurfer/api-client-java.svg" alt="JitPack"></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0-blue.svg" alt="License"></a>
</p>

Auto-generated Java API client for the [QTSurfer API](https://github.com/QTSurfer/qtsurfer-api), produced from the OpenAPI 3.1 spec with [openapi-generator](https://openapi-generator.tech) and the `java` + `native` library (JDK `java.net.http.HttpClient`).

This package is intentionally thin: one method per endpoint, 1:1 with the spec. For workflow orchestration (polling, retries, domain objects, unified errors), use [`net.qtsurfer:sdk`](https://github.com/QTSurfer/sdk-java).

- `java.net.http.HttpClient`-based; zero HTTP runtime dependencies beyond Jackson.
- Generated sources are written to `target/generated-sources/openapi` on every build (spec fetched from `main` of [`QTSurfer/qtsurfer-api`](https://github.com/QTSurfer/qtsurfer-api)).
- Requires **JDK 17+**.

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
  <groupId>com.github.QTSurfer</groupId>
  <artifactId>api-client-java</artifactId>
  <version>v0.1.0</version>
</dependency>
```

For Gradle:

```gradle
repositories { maven { url 'https://jitpack.io' } }
dependencies { implementation 'com.github.QTSurfer:api-client-java:v0.1.0' }
```

### Via Maven Central (future)

Once published to Central, the coordinate will be `net.qtsurfer:api-client:0.1.0`.

## Quick start

```java
import net.qtsurfer.api.client.api.ExchangeApi;
import net.qtsurfer.api.client.invoker.ApiClient;
import net.qtsurfer.api.client.model.Exchange;

import java.util.List;

ApiClient client = new ApiClient();
client.updateBaseUri("https://api.qtsurfer.net/v1");
client.setRequestInterceptor(builder ->
    builder.header("Authorization", "Bearer " + System.getenv("JWT_API_TOKEN")));

ExchangeApi exchanges = new ExchangeApi(client);
List<Exchange> result = exchanges.getExchanges();
```

## API surface

| API class | Methods |
| --- | --- |
| `ExchangeApi` | `getExchanges()` |
| `InstrumentApi` | `getInstruments(exchangeId)` |
| `StrategyApi` | `postStrategy(body, xCompileAsync)`, `getStrategyStatus(strategyId)` |
| `BacktestingApi` | `prepareBacktesting`, `getPreparationStatus`, `executeBacktesting`, `cancelExecution`, `getExecutionResult` |

All generated model types (`Exchange`, `InstrumentDetail`, `JobState`, `BacktestJobResult`, `ResultMap`, `ResponseError`, …) live under `net.qtsurfer.api.client.model`.

## Configuring the client

`ApiClient` exposes the underlying `HttpClient.Builder` and an `ObjectMapper`, plus hooks for request/response interceptors.

```java
client.updateBaseUri("https://api.qtsurfer.net/v1");

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
