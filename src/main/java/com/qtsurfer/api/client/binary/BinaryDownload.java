package com.qtsurfer.api.client.binary;

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpHeaders;
import java.util.Optional;

/**
 * An unbuffered hourly download and its response headers. Close it after consuming the body.
 * A provisional response may grow; retain its status and data-through time when caching it.
 *
 * @param body binary response stream, without timestamp conversion
 * @param headers HTTP response headers
 */
public record BinaryDownload(InputStream body, HttpHeaders headers) implements AutoCloseable {
    /** @return the declared binary media type */
    public Optional<String> contentType() { return headers.firstValue("Content-Type"); }

    /** @return the Lastra timestamp unit, normally {@code micros}; Parquet uses its own schema */
    public Optional<String> timestampUnit() { return headers.firstValue("X-Ts-Unit"); }

    /** @return {@code provisional} when served data can still grow, or empty when not supplied */
    public Optional<String> dataStatus() { return headers.firstValue("X-Data-Status"); }

    /** @return the last row's ISO-8601 UTC instant, regardless of the encoded timestamp unit */
    public Optional<String> dataThrough() { return headers.firstValue("X-Data-Through"); }

    @Override
    public void close() throws IOException { body.close(); }
}
