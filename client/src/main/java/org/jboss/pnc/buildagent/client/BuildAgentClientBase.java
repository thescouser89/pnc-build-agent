package org.jboss.pnc.buildagent.client;

import org.jboss.logging.Logger;
import org.jboss.pnc.buildagent.api.httpinvoke.RetryConfig;
import org.jboss.pnc.buildagent.common.StringUtils;
import org.jboss.pnc.buildagent.common.http.HttpClient;

import java.io.Closeable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static io.undertow.util.Headers.CONTENT_LENGTH_STRING;
import static org.jboss.pnc.buildagent.api.Constants.FILE_DOWNLOAD_PATH;
import static org.jboss.pnc.buildagent.api.Constants.FILE_UPLOAD_PATH;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public abstract class BuildAgentClientBase implements Closeable {

    private final Logger log = Logger.getLogger(BuildAgentClientBase.class);

    private final Optional<HttpClient> internalHttpClient;
    private final Optional<HttpClient> httpClient;
    protected final URI livenessProbeLocation;
    private final long livenessResponseTimeout;
    protected final RetryConfig retryConfig;
    private final URL fileUploadUrl;
    private final URL fileDownloadUrl;

    public BuildAgentClientBase(String termBaseUrl, long livenessResponseTimeout, RetryConfig retryConfig) throws BuildAgentClientException {
        this.livenessResponseTimeout = livenessResponseTimeout;
        this.retryConfig = retryConfig;
        termBaseUrl = StringUtils.stripEndingSlash(termBaseUrl);
        this.livenessProbeLocation = URI.create(termBaseUrl + "/servlet/is-alive");

        try {
            fileUploadUrl = new URL(termBaseUrl + FILE_UPLOAD_PATH);
        } catch (MalformedURLException e) {
            throw new BuildAgentClientException("Invalid file upload url.", e);
        }

        try {
            fileDownloadUrl = new URL(termBaseUrl + FILE_DOWNLOAD_PATH);
        } catch (MalformedURLException e) {
            throw new BuildAgentClientException("Invalid file download url.", e);
        }

        try {
            internalHttpClient = Optional.of(new HttpClient());
            httpClient = Optional.empty();
        } catch (IOException e) {
            throw new BuildAgentClientException("Cannot initialize http client.", e);
        }
    }

    /**
     * It is preferable to use a single instance of a HttpClient for all the BuildAgentClients because of the HttpClient's
     * internal thread pool.
     *
     * @param httpClient
     * @param termBaseUrl
     * @param livenessResponseTimeout
     */
    public BuildAgentClientBase(HttpClient httpClient, String termBaseUrl, long livenessResponseTimeout, RetryConfig retryConfig)
            throws BuildAgentClientException {
        this.livenessResponseTimeout = livenessResponseTimeout;
        this.retryConfig = retryConfig;
        termBaseUrl = StringUtils.stripEndingSlash(termBaseUrl);
        this.livenessProbeLocation = URI.create(termBaseUrl + "/servlet/is-alive");
        this.internalHttpClient = Optional.empty();
        this.httpClient = Optional.of(httpClient);

        try {
            fileUploadUrl = new URL(termBaseUrl + FILE_UPLOAD_PATH);
        } catch (MalformedURLException e) {
            throw new BuildAgentClientException("Invalid file upload url.", e);
        }

        try {
            fileDownloadUrl = new URL(termBaseUrl + FILE_DOWNLOAD_PATH);
        } catch (MalformedURLException e) {
            throw new BuildAgentClientException("Invalid file download url.", e);
        }
    }

    public boolean isServerAlive() {
        CompletableFuture<HttpClient.Response> responseFuture = new CompletableFuture<>();
        getHttpClient().invoke(livenessProbeLocation, "HEAD", "", responseFuture);
        try {
            HttpClient.Response response = responseFuture.get(livenessResponseTimeout, TimeUnit.MILLISECONDS);
            boolean isSuccess = response.getCode() == 200;
            return isSuccess;
        } catch (InterruptedException | TimeoutException | ExecutionException e) {
            log.warn("Did not receive liveness probe response.", e);
            responseFuture.cancel(true);
            return false;
        }
    }

    protected HttpClient getHttpClient() {
        if (internalHttpClient.isPresent()) {
            return internalHttpClient.get();
        } else {
            return httpClient.get();
        }
    }

    public void uploadFile(
            ByteBuffer buffer,
            Path remoteFilePath,
            CompletableFuture<HttpClient.Response> responseFuture) {
        Map<String, String> headers = Collections.emptyMap();
        URI uri;
        try {
            uri = new URI(fileUploadUrl.toString() + remoteFilePath.toString());
        } catch (URISyntaxException e) {
            responseFuture.completeExceptionally(e);
            return;
        }
        getHttpClient().invoke(
                uri,
                "PUT",
                headers,
                buffer,
                responseFuture,
                retryConfig.getMaxRetries(),
                retryConfig.getWaitBeforeRetry(),
                -1L
        );
    }

    public void downloadFile(
            Path remoteFilePath,
            CompletableFuture<HttpClient.Response> responseFuture) {
        downloadFile(remoteFilePath,responseFuture, -1L);
    }

    public void downloadFile(
            Path remoteFilePath,
            CompletableFuture<HttpClient.Response> responseFuture,
            long maxDownloadSize) {
        URI uri;
        try {
            uri = new URI(fileDownloadUrl + remoteFilePath.toString());
        } catch (URISyntaxException e) {
            responseFuture.completeExceptionally(e);
            return;
        }

        getHttpClient().invoke(
                uri,
                "GET",
                Collections.emptyMap(),
                ByteBuffer.allocate(0),
                responseFuture,
                retryConfig.getMaxRetries(),
                retryConfig.getWaitBeforeRetry(),
                maxDownloadSize
        );
    }

    @Override
    public void close() throws IOException {
        if (internalHttpClient.isPresent()) {
            internalHttpClient.get().close();
        }
    }
}
