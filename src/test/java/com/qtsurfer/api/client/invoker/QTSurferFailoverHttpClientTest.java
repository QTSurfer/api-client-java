package com.qtsurfer.api.client.invoker;

import java.io.IOException;
import java.net.ConnectException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpHeaders;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class QTSurferFailoverHttpClientTest {

    @Test
    void retriesApiConnectionFailureAndStartsTheNextRequestAtPrimary() throws Exception {
        RecordingClient delegate = new RecordingClient(true, 200);
        HttpClient client = QTSurferFailoverHttpClient.wrap(delegate);
        HttpRequest request = HttpRequest.newBuilder(URI.create("https://api.qtsurfer.net/v1/exchanges"))
                .GET().build();

        assertEquals(200, client.send(request, HttpResponse.BodyHandlers.ofString()).statusCode());
        assertEquals("api.qtsurfer.net", delegate.uris.get(0).getHost());
        assertEquals("api2.qtsurfer.net", delegate.uris.get(1).getHost());

        delegate.failPrimary = false;
        client.send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals("api.qtsurfer.net", delegate.uris.get(2).getHost());
    }

    @Test
    void retriesStorageUsingTheSecondaryHostAndPreservesSignedQuery() throws Exception {
        RecordingClient delegate = new RecordingClient(true, 200);
        HttpClient client = QTSurferFailoverHttpClient.wrap(delegate);
        URI signed = URI.create("https://storage.qtsurfer.net/uploads/a/raw.csv?X-Amz-Signature=a%2Fb&X-Amz-Expires=30");

        client.send(HttpRequest.newBuilder(signed).PUT(HttpRequest.BodyPublishers.ofString("data")).build(),
                HttpResponse.BodyHandlers.discarding());

        assertEquals("storage2.qtsurfer.net", delegate.uris.get(1).getHost());
        assertEquals(signed.getRawPath(), delegate.uris.get(1).getRawPath());
        assertEquals(signed.getRawQuery(), delegate.uris.get(1).getRawQuery());
    }

    @Test
    void replacesOnlyTheFirstLabelAndPreservesTheDomainSuffix() {
        URI comFallback = QTSurferFailoverHttpClient
                .fallbackUriForTest(URI.create("https://api.qtsurfer.com/v1/jobs"));
        assertEquals("api2.qtsurfer.com", comFallback.getHost());
        assertEquals("/v1/jobs", comFallback.getRawPath());
        assertEquals("storage2.qtsurfer.net", QTSurferFailoverHttpClient
                .fallbackUriForTest(URI.create("https://storage.qtsurfer.net/a?sig=x")).getHost());
        assertNull(QTSurferFailoverHttpClient
                .fallbackUriForTest(URI.create("https://api2.qtsurfer.net/v1/jobs")));
        assertNull(QTSurferFailoverHttpClient
                .fallbackUriForTest(URI.create("https://storage2.qtsurfer.com/a?sig=x")));
        assertNull(QTSurferFailoverHttpClient
                .fallbackUriForTest(URI.create("https://myapi.qtsurfer.net/v1/jobs")));
    }

    @Test
    void doesNotRetryHttpResponsesOrUnrelatedHosts() throws Exception {
        RecordingClient delegate = new RecordingClient(false, 503);
        HttpClient client = QTSurferFailoverHttpClient.wrap(delegate);
        client.send(HttpRequest.newBuilder(URI.create("https://api.qtsurfer.net/v1/exchanges")).GET().build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(1, delegate.uris.size());

        delegate.failPrimary = true;
        assertThrows(ConnectException.class, () -> client.send(
                HttpRequest.newBuilder(URI.create("https://example.com/path")).GET().build(),
                HttpResponse.BodyHandlers.ofString()));
        assertEquals(2, delegate.uris.size());
    }

    private static final class RecordingClient extends HttpClient {
        private final List<URI> uris = new ArrayList<>();
        private boolean failPrimary;
        private final int status;

        private RecordingClient(boolean failPrimary, int status) {
            this.failPrimary = failPrimary;
            this.status = status;
        }

        @Override public Optional<java.net.CookieHandler> cookieHandler() { return Optional.empty(); }
        @Override public Optional<Duration> connectTimeout() { return Optional.empty(); }
        @Override public Redirect followRedirects() { return Redirect.NEVER; }
        @Override public Optional<java.net.ProxySelector> proxy() { return Optional.empty(); }
        @Override public SSLContext sslContext() {
            try { return SSLContext.getDefault(); }
            catch (java.security.NoSuchAlgorithmException error) { throw new AssertionError(error); }
        }
        @Override public SSLParameters sslParameters() { return new SSLParameters(); }
        @Override public Optional<java.net.Authenticator> authenticator() { return Optional.empty(); }
        @Override public Version version() { return Version.HTTP_1_1; }
        @Override public Optional<Executor> executor() { return Optional.empty(); }

        @Override
        public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler)
                throws IOException {
            uris.add(request.uri());
            String host = request.uri().getHost();
            String label = host.substring(0, host.indexOf('.'));
            if (failPrimary && (label.equals("api") || label.equals("storage")
                    || host.equals("example.com"))) {
                throw new ConnectException("unreachable");
            }
            return new Response<>(request, null, status);
        }

        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request, HttpResponse.BodyHandler<T> handler) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException());
        }

        @Override public <T> CompletableFuture<HttpResponse<T>> sendAsync(
                HttpRequest request, HttpResponse.BodyHandler<T> handler, HttpResponse.PushPromiseHandler<T> push) {
            return CompletableFuture.failedFuture(new UnsupportedOperationException());
        }
    }

    private record Response<T>(HttpRequest request, T body, int status) implements HttpResponse<T> {
        @Override public int statusCode() { return status; }
        @Override public Optional<HttpResponse<T>> previousResponse() { return Optional.empty(); }
        @Override public HttpHeaders headers() { return HttpHeaders.of(Map.of(), (a, b) -> true); }
        @Override public HttpRequest request() { return request; }
        @Override public Optional<javax.net.ssl.SSLSession> sslSession() { return Optional.empty(); }
        @Override public URI uri() { return request.uri(); }
        @Override public HttpClient.Version version() { return HttpClient.Version.HTTP_1_1; }
    }
}
