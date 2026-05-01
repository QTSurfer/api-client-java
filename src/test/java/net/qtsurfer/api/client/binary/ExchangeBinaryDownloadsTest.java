package net.qtsurfer.api.client.binary;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import net.qtsurfer.api.client.invoker.ApiClient;
import net.qtsurfer.api.client.invoker.ApiException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExchangeBinaryDownloadsTest {

    private HttpServer server;
    private ApiClient apiClient;
    private final List<HttpExchange> exchanges = new ArrayList<>();
    private final AtomicReference<byte[]> responseBody = new AtomicReference<>(new byte[0]);
    private final AtomicReference<Integer> responseStatus = new AtomicReference<>(200);

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            exchanges.add(exchange);
            byte[] body = responseBody.get();
            int status = responseStatus.get();
            exchange.getResponseHeaders().add("Content-Type", "application/vnd.lastra");
            exchange.sendResponseHeaders(status, body.length == 0 ? -1 : body.length);
            try (var os = exchange.getResponseBody()) {
                if (body.length > 0) os.write(body);
            }
        });
        server.start();
        apiClient = new ApiClient();
        apiClient.updateBaseUri("http://127.0.0.1:" + server.getAddress().getPort());
    }

    @AfterEach
    void stop() {
        server.stop(0);
    }

    @Test
    void getTickersHourReturnsBodyAsStream() throws Exception {
        byte[] payload = "LASTRA-BYTES".getBytes(StandardCharsets.UTF_8);
        responseBody.set(payload);

        ExchangeBinaryDownloads downloads = new ExchangeBinaryDownloads(apiClient);
        try (InputStream in = downloads.getTickersHour("binance", "BTC", "USDT", "2026-01-15T10")) {
            assertArrayEquals(payload, in.readAllBytes());
        }

        HttpExchange recorded = exchanges.get(0);
        assertEquals("GET", recorded.getRequestMethod());
        assertEquals("/exchange/binance/tickers/BTC/USDT", recorded.getRequestURI().getPath());
        assertEquals("hour=2026-01-15T10", recorded.getRequestURI().getRawQuery());
    }

    @Test
    void getKlinesHourAddsFormatQueryParam() throws Exception {
        responseBody.set("PARQUET".getBytes(StandardCharsets.UTF_8));

        ExchangeBinaryDownloads downloads = new ExchangeBinaryDownloads(apiClient);
        try (InputStream in = downloads.getKlinesHour(
                "binance", "BTC", "USDT", "2026-01-15T10", ExchangeBinaryDownloads.Format.PARQUET)) {
            assertNotNull(in);
            in.readAllBytes();
        }

        HttpExchange recorded = exchanges.get(0);
        assertEquals("/exchange/binance/klines/BTC/USDT", recorded.getRequestURI().getPath());
        assertEquals("hour=2026-01-15T10&format=parquet", recorded.getRequestURI().getRawQuery());
    }

    @Test
    void appliesBearerTokenFromRequestInterceptor() throws Exception {
        responseBody.set("ok".getBytes(StandardCharsets.UTF_8));
        apiClient.setRequestInterceptor(b -> b.header("Authorization", "Bearer test-token"));

        ExchangeBinaryDownloads downloads = new ExchangeBinaryDownloads(apiClient);
        try (InputStream in = downloads.getTickersHour("binance", "BTC", "USDT", "2026-01-15T10")) {
            in.readAllBytes();
        }

        assertEquals("Bearer test-token", exchanges.get(0).getRequestHeaders().getFirst("Authorization"));
    }

    @Test
    void urlEncodesPathAndQueryComponents() throws Exception {
        responseBody.set("ok".getBytes(StandardCharsets.UTF_8));

        ExchangeBinaryDownloads downloads = new ExchangeBinaryDownloads(apiClient);
        try (InputStream in = downloads.getTickersHour("ex space", "B/T C", "U+S D", "2026-01-15T10")) {
            in.readAllBytes();
        }

        HttpExchange recorded = exchanges.get(0);
        // URLEncoder emits + for spaces and percent-encodes special characters; the path encoder
        // is intentionally lossy on slashes inside path params (callers shouldn't pass them).
        assertTrue(recorded.getRequestURI().getRawPath().contains("ex+space")
                || recorded.getRequestURI().getRawPath().contains("ex%20space"));
        assertTrue(recorded.getRequestURI().getRawPath().contains("U%2BS+D")
                || recorded.getRequestURI().getRawPath().contains("U%2BS%20D"));
    }

    @Test
    void non2xxResponseThrowsApiException() {
        responseStatus.set(404);
        responseBody.set("{\"code\":\"NOT_FOUND\",\"message\":\"hour not backfilled\"}".getBytes(StandardCharsets.UTF_8));

        ExchangeBinaryDownloads downloads = new ExchangeBinaryDownloads(apiClient);
        ApiException ex = assertThrows(
                ApiException.class,
                () -> downloads.getTickersHour("binance", "BTC", "USDT", "2026-01-15T10"));
        assertEquals(404, ex.getCode());
        assertTrue(ex.getMessage().contains("getTickersHour"));
        assertTrue(ex.getMessage().contains("NOT_FOUND"));
    }

    @Test
    void rejectsBlankRequiredParameters() {
        ExchangeBinaryDownloads downloads = new ExchangeBinaryDownloads(apiClient);
        ApiException ex = assertThrows(
                ApiException.class,
                () -> downloads.getTickersHour("", "BTC", "USDT", "2026-01-15T10"));
        assertEquals(400, ex.getCode());
        assertTrue(ex.getMessage().contains("exchangeId"));
    }

    @Test
    void formatEnumExposesWireValue() {
        assertEquals("lastra", ExchangeBinaryDownloads.Format.LASTRA.wireValue());
        assertEquals("parquet", ExchangeBinaryDownloads.Format.PARQUET.wireValue());
    }
}
