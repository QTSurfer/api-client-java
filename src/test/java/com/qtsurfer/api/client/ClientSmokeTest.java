package com.qtsurfer.api.client;

import com.qtsurfer.api.client.invoker.ApiClient;
import com.qtsurfer.api.client.invoker.QTSurferFailoverHttpClient;
import java.net.http.HttpClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Smoke test: verifies the generated client instantiates and honors custom configuration.
 * No network calls are made.
 */
class ClientSmokeTest {

    @Test
    void instantiatesWithDefaults() {
        ApiClient client = new ApiClient();
        assertNotNull(client);
        assertNotNull(client.getBaseUri());
        assertInstanceOf(QTSurferFailoverHttpClient.class, client.getHttpClient());
    }

    @Test
    void honorsCustomBaseUri() {
        ApiClient client = new ApiClient();
        client.updateBaseUri("https://api.qtsurfer.net/v1");
        assertEquals("https://api.qtsurfer.net/v1", client.getBaseUri());
    }

    @Test
    void wrapsCustomHttpClientBuilders() {
        ApiClient client = new ApiClient();
        HttpClient.Builder customBuilder = HttpClient.newBuilder();
        client.setHttpClientBuilder(customBuilder);
        assertInstanceOf(QTSurferFailoverHttpClient.class, client.getHttpClient());

        HttpClient.Builder defaultBuilder = ApiClient.createDefaultHttpClientBuilder();
        assertSame(defaultBuilder, QTSurferFailoverHttpClient.builder(defaultBuilder));
    }

    @Test
    void acceptsBearerTokenViaRequestInterceptor() {
        ApiClient client = new ApiClient();
        client.setRequestInterceptor(builder -> builder.header("Authorization", "Bearer test-token"));
        assertNotNull(client.getRequestInterceptor());
    }
}
