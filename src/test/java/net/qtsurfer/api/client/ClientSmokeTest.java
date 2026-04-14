package net.qtsurfer.api.client;

import net.qtsurfer.api.client.invoker.ApiClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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
    }

    @Test
    void honorsCustomBaseUri() {
        ApiClient client = new ApiClient();
        client.updateBaseUri("https://api.qtsurfer.net/v1");
        assertEquals("https://api.qtsurfer.net/v1", client.getBaseUri());
    }

    @Test
    void acceptsBearerTokenViaRequestInterceptor() {
        ApiClient client = new ApiClient();
        client.setRequestInterceptor(builder -> builder.header("Authorization", "Bearer test-token"));
        assertNotNull(client.getRequestInterceptor());
    }
}
