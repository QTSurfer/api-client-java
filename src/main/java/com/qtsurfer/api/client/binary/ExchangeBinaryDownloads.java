package com.qtsurfer.api.client.binary;

import com.qtsurfer.api.client.invoker.ApiClient;
import com.qtsurfer.api.client.invoker.ApiException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * Streaming downloads for the binary endpoints
 * {@code GET /exchange/{ex}/tickers/{base}/{quote}} and {@code GET /exchange/{ex}/klines/{base}/{quote}}.
 *
 * <p>The auto-generated {@code ExchangeApi#getExchangeTickersHour} and {@code getExchangeKlinesHour}
 * methods are unusable for binary payloads: openapi-generator's {@code native} library decodes the
 * body as a UTF-8 string and feeds it to Jackson, which corrupts {@code application/vnd.lastra}
 * and {@code application/vnd.apache.parquet} bytes. This class is the manual escape hatch — it
 * issues the request through {@link ApiClient}'s HTTP client and request interceptor (so auth
 * keeps working) and returns the response body as an {@link InputStream} so callers can stream
 * straight into a Lastra reader, a file, or any other consumer.
 */
public final class ExchangeBinaryDownloads {

    /** Wire format selector for the {@code format} query parameter. */
    public enum Format {
        LASTRA("lastra"),
        PARQUET("parquet");

        private final String wireValue;

        Format(String wireValue) {
            this.wireValue = wireValue;
        }

        public String wireValue() {
            return wireValue;
        }
    }

    private final HttpClient httpClient;
    private final String baseUri;
    private final Consumer<HttpRequest.Builder> requestInterceptor;

    public ExchangeBinaryDownloads() {
        this(new ApiClient());
    }

    public ExchangeBinaryDownloads(ApiClient apiClient) {
        Objects.requireNonNull(apiClient, "apiClient");
        this.httpClient = apiClient.getHttpClient();
        this.baseUri = apiClient.getBaseUri();
        this.requestInterceptor = apiClient.getRequestInterceptor();
    }

    /**
     * Download one hour of tickers as a Lastra (or Parquet) stream.
     *
     * @param exchangeId exchange id, e.g. {@code "binance"}
     * @param base       base asset symbol
     * @param quote      quote asset symbol
     * @param hour       hour selector, {@code YYYY-MM-DDTHH} in UTC
     * @param format     wire format; {@code null} defaults to {@link Format#LASTRA}
     * @return the response body stream (caller closes it)
     * @throws ApiException on non-2xx responses or transport failure
     */
    public InputStream getTickersHour(String exchangeId, String base, String quote, String hour, Format format)
            throws ApiException {
        return download("getTickersHour", "tickers", exchangeId, base, quote, hour, format);
    }

    /** Same as {@link #getTickersHour} with the default Lastra format. */
    public InputStream getTickersHour(String exchangeId, String base, String quote, String hour) throws ApiException {
        return getTickersHour(exchangeId, base, quote, hour, null);
    }

    /**
     * Download one hour of klines as a Lastra (or Parquet) stream.
     *
     * @see #getTickersHour
     */
    public InputStream getKlinesHour(String exchangeId, String base, String quote, String hour, Format format)
            throws ApiException {
        return download("getKlinesHour", "klines", exchangeId, base, quote, hour, format);
    }

    /** Same as {@link #getKlinesHour} with the default Lastra format. */
    public InputStream getKlinesHour(String exchangeId, String base, String quote, String hour) throws ApiException {
        return getKlinesHour(exchangeId, base, quote, hour, null);
    }

    private InputStream download(
            String operationId,
            String resource,
            String exchangeId,
            String base,
            String quote,
            String hour,
            Format format) throws ApiException {
        requireNonBlank(exchangeId, "exchangeId", operationId);
        requireNonBlank(base, "base", operationId);
        requireNonBlank(quote, "quote", operationId);
        requireNonBlank(hour, "hour", operationId);

        StringBuilder url = new StringBuilder(baseUri)
                .append("/exchange/").append(encode(exchangeId))
                .append('/').append(resource)
                .append('/').append(encode(base))
                .append('/').append(encode(quote))
                .append("?hour=").append(encode(hour));
        if (format != null) {
            url.append("&format=").append(format.wireValue());
        }

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(url.toString()))
                .GET();
        if (requestInterceptor != null) {
            requestInterceptor.accept(builder);
        }

        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(builder.build(), HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException e) {
            throw new ApiException(e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ApiException(e);
        }

        if (response.statusCode() / 100 != 2) {
            throw asApiException(operationId, response);
        }
        return response.body();
    }

    private static ApiException asApiException(String operationId, HttpResponse<InputStream> response) {
        String body = "";
        try (InputStream in = response.body()) {
            if (in != null) {
                body = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            }
        } catch (IOException ignored) {
            // best-effort: fall through with whatever we managed to read
        }
        String displayBody = body.isEmpty() ? "[no body]" : body;
        String message = operationId + " call failed with: " + response.statusCode() + " - " + displayBody;
        return new ApiException(response.statusCode(), message, response.headers(), body);
    }

    private static void requireNonBlank(String value, String name, String operationId) throws ApiException {
        if (value == null || value.isEmpty()) {
            throw new ApiException(400, "Missing the required parameter '" + name + "' when calling " + operationId);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
