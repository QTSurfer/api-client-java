package com.qtsurfer.api.client;

import com.qtsurfer.api.client.api.AuthApi;
import com.qtsurfer.api.client.invoker.ApiClient;
import com.qtsurfer.api.client.invoker.ApiException;
import com.qtsurfer.api.client.model.AuthTokenError;
import com.qtsurfer.api.client.model.AuthTokenResponse;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Authentication status and body contracts, served by a local HTTP fixture. */
class AuthContractTest {
    private HttpServer server;
    private ApiClient client;
    private volatile int responseStatus;
    private volatile String responseBody;
    private final AtomicInteger calls = new AtomicInteger();

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/auth/token", exchange -> {
            calls.incrementAndGet();
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, body.length);
            try (var output = exchange.getResponseBody()) {
                output.write(body);
            }
        });
        server.start();
        client = new ApiClient();
        client.updateBaseUri("http://127.0.0.1:" + server.getAddress().getPort());
        client.setRequestInterceptor(builder -> builder.header("X-API-Key", "test-key"));
    }

    @AfterEach
    void stop() { server.stop(0); }

    @Test
    void scopeFreeKeyRejectionPreserves403AndTypedReasonWithoutRetry() throws Exception {
        responseStatus = 403;
        responseBody = """
                {"code":"apikey_without_scopes",
                "message":"This API key has no scopes. Create a key with at least one scope in the web app."}
                """;

        ApiException failure = assertThrows(ApiException.class, () -> new AuthApi(client).authenticate());

        assertEquals(403, failure.getCode());
        AuthTokenError error = client.getObjectMapper().readValue(failure.getResponseBody(), AuthTokenError.class);
        assertEquals(AuthTokenError.CodeEnum.APIKEY_WITHOUT_SCOPES, error.getCode());
        assertEquals("apikey_without_scopes", error.getCode().getValue());
        assertTrue(error.getMessage().contains("at least one scope"));
        assertEquals("apikey_without_scopes", client.getObjectMapper()
                .valueToTree(error).get("code").asText());
        assertEquals(1, calls.get());
    }

    @Test
    void successfulTokenStillAcceptsAnEmptyScopesMap() throws Exception {
        responseStatus = 200;
        responseBody = """
                {"access_token":"test-token","token_type":"Bearer","expires_in":3600,
                "tier":"free","scopes":{}}
                """;

        AuthTokenResponse token = new AuthApi(client).authenticate();

        assertEquals("test-token", token.getAccessToken());
        assertTrue(token.getScopes().isEmpty());
        assertTrue(client.getObjectMapper().valueToTree(token).get("scopes").isObject());
        assertEquals(1, calls.get());
    }
}
