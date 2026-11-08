package com.qtsurfer.api.client.invoker;

import java.io.IOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.Authenticator;
import java.net.CookieHandler;
import java.net.ProxySelector;
import java.net.http.HttpClient;
import java.net.http.HttpConnectTimeoutException;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.Executor;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLParameters;

/** HTTP transport that retries one connection failure against QTSurfer's secondary hosts. */
public final class QTSurferFailoverHttpClient extends HttpClient {
    private final HttpClient delegate;

    private QTSurferFailoverHttpClient(HttpClient delegate) {
        this.delegate = delegate;
    }

    /** Wrap an HTTP client with per-request API and object-storage failover. */
    public static HttpClient wrap(HttpClient client) {
        return client instanceof QTSurferFailoverHttpClient ? client : new QTSurferFailoverHttpClient(client);
    }

    /** Build a failover-enabled client while preserving all options on the supplied builder. */
    public static HttpClient.Builder builder(HttpClient.Builder builder) {
        return builder instanceof FailoverBuilder ? builder : new FailoverBuilder(builder);
    }

    @Override
    public Optional<CookieHandler> cookieHandler() { return delegate.cookieHandler(); }

    @Override
    public Optional<Duration> connectTimeout() { return delegate.connectTimeout(); }

    @Override
    public HttpClient.Redirect followRedirects() { return delegate.followRedirects(); }

    @Override
    public Optional<ProxySelector> proxy() { return delegate.proxy(); }

    @Override
    public SSLContext sslContext() { return delegate.sslContext(); }

    @Override
    public SSLParameters sslParameters() { return delegate.sslParameters(); }

    @Override
    public Optional<Authenticator> authenticator() { return delegate.authenticator(); }

    @Override
    public HttpClient.Version version() { return delegate.version(); }

    @Override
    public Optional<Executor> executor() { return delegate.executor(); }

    @Override
    public <T> HttpResponse<T> send(HttpRequest request, HttpResponse.BodyHandler<T> handler)
            throws IOException, InterruptedException {
        try {
            return delegate.send(request, handler);
        } catch (IOException error) {
            URI fallback = fallbackUri(request.uri());
            if (fallback == null || !isConnectionFailure(error)) throw error;
            return delegate.send(copyWithUri(request, fallback), handler);
        }
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(
            HttpRequest request, HttpResponse.BodyHandler<T> handler) {
        return sendAsync(request, handler, null);
    }

    @Override
    public <T> CompletableFuture<HttpResponse<T>> sendAsync(
            HttpRequest request,
            HttpResponse.BodyHandler<T> handler,
            HttpResponse.PushPromiseHandler<T> pushPromiseHandler) {
        CompletableFuture<HttpResponse<T>> first = pushPromiseHandler == null
                ? delegate.sendAsync(request, handler)
                : delegate.sendAsync(request, handler, pushPromiseHandler);
        return first.handle((response, failure) -> {
            if (failure == null) return CompletableFuture.completedFuture(response);
            Throwable cause = unwrap(failure);
            URI fallback = fallbackUri(request.uri());
            if (fallback == null || !isConnectionFailure(cause)) {
                return CompletableFuture.<HttpResponse<T>>failedFuture(cause);
            }
            HttpRequest retry = copyWithUri(request, fallback);
            return pushPromiseHandler == null
                    ? delegate.sendAsync(retry, handler)
                    : delegate.sendAsync(retry, handler, pushPromiseHandler);
        }).thenCompose(future -> future);
    }

    private static URI fallbackUri(URI uri) {
        if (uri == null) return null;
        String host = uri.getHost();
        if (host == null) return null;
        int suffixStart = host.indexOf('.');
        if (suffixStart < 0) return null;
        String label = host.substring(0, suffixStart);
        if (!label.equalsIgnoreCase("api") && !label.equalsIgnoreCase("storage")) return null;
        String alternate = label + "2" + host.substring(suffixStart);
        String authority = alternate + (uri.getPort() < 0 ? "" : ":" + uri.getPort());
        String target = uri.getScheme() + "://" + authority
                + (uri.getRawPath() == null ? "" : uri.getRawPath())
                + (uri.getRawQuery() == null ? "" : "?" + uri.getRawQuery())
                + (uri.getRawFragment() == null ? "" : "#" + uri.getRawFragment());
        return URI.create(target);
    }

    static URI fallbackUriForTest(URI uri) {
        return fallbackUri(uri);
    }

    private static HttpRequest copyWithUri(HttpRequest request, URI uri) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri);
        request.timeout().ifPresent(builder::timeout);
        request.version().ifPresent(builder::version);
        if (request.expectContinue()) builder.expectContinue(true);
        request.headers().map().forEach((name, values) -> values.forEach(value -> builder.header(name, value)));
        builder.method(request.method(), request.bodyPublisher().orElse(HttpRequest.BodyPublishers.noBody()));
        return builder.build();
    }

    private static boolean isConnectionFailure(Throwable error) {
        for (Throwable current = error; current != null; current = current.getCause()) {
            if (current instanceof ConnectException
                    || current instanceof HttpConnectTimeoutException
                    || current instanceof UnknownHostException
                    || current instanceof NoRouteToHostException) return true;
        }
        return false;
    }

    private static Throwable unwrap(Throwable failure) {
        Throwable current = failure;
        while ((current instanceof CompletionException) && current.getCause() != null) current = current.getCause();
        return current;
    }

    private static final class FailoverBuilder implements HttpClient.Builder {
        private final HttpClient.Builder delegate;

        private FailoverBuilder(HttpClient.Builder delegate) { this.delegate = delegate; }

        @Override public HttpClient.Builder cookieHandler(CookieHandler handler) { delegate.cookieHandler(handler); return this; }
        @Override public HttpClient.Builder connectTimeout(Duration duration) { delegate.connectTimeout(duration); return this; }
        @Override public HttpClient.Builder sslContext(SSLContext context) { delegate.sslContext(context); return this; }
        @Override public HttpClient.Builder sslParameters(SSLParameters parameters) { delegate.sslParameters(parameters); return this; }
        @Override public HttpClient.Builder executor(Executor executor) { delegate.executor(executor); return this; }
        @Override public HttpClient.Builder followRedirects(HttpClient.Redirect policy) { delegate.followRedirects(policy); return this; }
        @Override public HttpClient.Builder version(HttpClient.Version version) { delegate.version(version); return this; }
        public HttpClient.Builder priority(int priority) {
            try {
                HttpClient.Builder.class.getMethod("priority", int.class).invoke(delegate, priority);
                return this;
            } catch (ReflectiveOperationException error) {
                throw new UnsupportedOperationException("This JDK HTTP client builder does not support priority", error);
            }
        }
        @Override public HttpClient.Builder proxy(ProxySelector selector) { delegate.proxy(selector); return this; }
        @Override public HttpClient.Builder authenticator(Authenticator authenticator) { delegate.authenticator(authenticator); return this; }
        @Override public HttpClient build() { return wrap(delegate.build()); }
    }
}
