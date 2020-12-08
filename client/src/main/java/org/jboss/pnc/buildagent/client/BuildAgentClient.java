package org.jboss.pnc.buildagent.client;

import org.jboss.pnc.buildagent.api.httpinvoke.RetryConfig;
import org.jboss.pnc.buildagent.common.http.HttpClient;

import java.io.Closeable;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public interface BuildAgentClient extends Closeable {

    void execute(Object command) throws BuildAgentClientException;

    void uploadFile(
            ByteBuffer buffer,
            Path remoteFilePath,
            CompletableFuture<HttpClient.Response> responseFuture);

    void downloadFile(
            Path remoteFilePath,
            CompletableFuture<HttpClient.Response> responseFuture);

    void downloadFile(
            Path remoteFilePath,
            CompletableFuture<HttpClient.Response> responseFuture,
            long maxDownloadSize);

    /**
     *
     * @param command
     * @param executeTimeout Total time to wait for a remote invocation. Note there can be multiple internal retries.
     * @param unit
     * @throws BuildAgentClientException
     */
    void execute(Object command, long executeTimeout, TimeUnit unit) throws BuildAgentClientException;

    void cancel() throws BuildAgentClientException;

    String getSessionId();

    boolean isServerAlive();
}
