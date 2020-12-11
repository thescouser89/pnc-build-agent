package org.jboss.pnc.buildagent.common.http;

import io.undertow.client.ClientCallback;
import io.undertow.client.ClientConnection;
import io.undertow.client.ClientExchange;
import io.undertow.client.ClientRequest;
import io.undertow.client.UndertowClient;
import io.undertow.connector.ByteBufferPool;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;
import org.jboss.pnc.buildagent.common.concurrent.MDCScheduledThreadPoolExecutor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;

import java.io.Closeable;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class HttpClient implements Closeable {
    private final Logger logger = LoggerFactory.getLogger(HttpClient.class);

    private final XnioWorker xnioWorker;

    private final ByteBufferPool buffer;

    public static final OptionMap DEFAULT_OPTIONS;

    private ScheduledExecutorService executor = new MDCScheduledThreadPoolExecutor(4);

    static {
        final OptionMap.Builder builder = OptionMap.builder()
                .set(Options.WORKER_IO_THREADS, 8)
                .set(Options.TCP_NODELAY, true)
                .set(Options.KEEP_ALIVE, true)
                .set(Options.WORKER_NAME, "Build Agent Http Client")
                .set(Options.READ_TIMEOUT, 30000)
                .set(Options.WRITE_TIMEOUT, 30000);
        DEFAULT_OPTIONS = builder.getMap();
    }

    public HttpClient() throws IOException {
        final Xnio xnio = Xnio.getInstance();
        xnioWorker = xnio.createWorker(null, DEFAULT_OPTIONS);

        buffer = new DefaultByteBufferPool(
                true,
                1024 * 16,
                1000,
                10,
                100);
    }

    public HttpClient(XnioWorker xnioWorker, ByteBufferPool buffer) throws IOException {
        this.xnioWorker = xnioWorker;
        this.buffer = buffer;
    }

    public void invoke(URI uri, String requestMethod, String data, CompletableFuture<Response> responseFuture) {
        logger.debug("Making {} request to the endpoint {}; request data: {}.", requestMethod, uri.toString(), data);
        invokeAttempt(uri, requestMethod, Collections.emptyMap(), ByteBuffer.wrap(data.getBytes(UTF_8)), responseFuture, 0, 0, 0L, -1L);
    }

    public void invoke(
            URI uri,
            String requestMethod,
            Map<String, String> requestHeaders,
            String data,
            CompletableFuture<Response> responseFuture,
            int maxRetries,
            long waitBeforeRetry) {
        logger.info("Making request {} {}; Headers: {} request data: {}.",
                requestMethod, uri.toString(), requestHeaders, data);
        invokeAttempt(uri, requestMethod, requestHeaders, ByteBuffer.wrap(data.getBytes(UTF_8)), responseFuture, 0, maxRetries, waitBeforeRetry, -1L);
    }

    public void invoke(
            URI uri,
            String requestMethod,
            Map<String, String> requestHeaders,
            String data,
            CompletableFuture<Response> responseFuture,
            int maxRetries,
            long waitBeforeRetry,
            long maxDownloadSize) {
        logger.info("Making request {} {}; Headers: {} request data: {}.",
                requestMethod, uri.toString(), requestHeaders, data);
        invokeAttempt(uri, requestMethod, requestHeaders, ByteBuffer.wrap(data.getBytes(UTF_8)), responseFuture, 0, maxRetries, waitBeforeRetry, maxDownloadSize);
    }

    public void invoke(
            URI uri,
            String requestMethod,
            Map<String, String> requestHeaders,
            ByteBuffer data,
            CompletableFuture<Response> responseFuture,
            int maxRetries,
            long waitBeforeRetry,
            long maxDownloadSize) {
        logger.info("Making request {} {}; Headers: {} request data: {}.",
                requestMethod, uri.toString(), requestHeaders, data);
        invokeAttempt(uri, requestMethod, requestHeaders, data, responseFuture, 0, maxRetries, waitBeforeRetry, maxDownloadSize);
    }

    /**
     *
     * @param uri
     * @param requestMethod
     * @param requestHeaders
     * @param data
     * @param responseFuture
     * @param attempt
     * @param maxRetries
     * @param waitBeforeRetry Wait before retry is calculated as attempt * waitBeforeRetry[millis].
     */
    private void invokeAttempt(
            URI uri,
            String requestMethod,
            Map<String, String> requestHeaders,
            ByteBuffer data,
            CompletableFuture<Response> responseFuture,
            int attempt,
            int maxRetries,
            long waitBeforeRetry,
            long maxDownloadSize) {
        if (attempt > 0) {
            logger.warn(
                    "Retrying ({}) {} request to the endpoint {}; request data: {}.",
                    attempt,
                    requestMethod,
                    uri.toString(),
                    data);
        }

        UndertowClient undertowClient = UndertowClient.getInstance();

        //see https://github.com/undertow-io/undertow/blob/master/core/src/test/java/io/undertow/client/http/HttpClientTestCase.java
        CompletableFuture<ClientConnection> clientConnectionFuture = new CompletableFuture<>();
        ClientCallback<ClientConnection> clientConnection = new ClientCallback<ClientConnection>() {
            @Override
            public void completed(ClientConnection connection) {
                clientConnectionFuture.complete(connection);
            }
            @Override
            public void failed(IOException e) {
                clientConnectionFuture.completeExceptionally(e);
            }
        };

        CompletableFuture<ClientExchange> onRequestStartFuture = new CompletableFuture<>();
        ClientCallback<ClientExchange> onRequestStart = new ClientCallback<ClientExchange>() {
            @Override
            public void completed(ClientExchange exchange) {
                onRequestStartFuture.complete(exchange);
            }
            @Override
            public void failed(IOException e) {
                onRequestStartFuture.completeExceptionally(e);
            }
        };

        CompletableFuture<ClientExchange> onResponseStartedFuture = new CompletableFuture<>();
        ClientCallback<ClientExchange> onResponseStarted = new ClientCallback<ClientExchange>() {
            @Override
            public void completed(ClientExchange exchange) {
                onResponseStartedFuture.complete(exchange);
            }
            @Override
            public void failed(IOException e) {
                onResponseStartedFuture.completeExceptionally(e);
            }
        };

        CompletableFuture<StringResult> onResponseCompletedFuture = new CompletableFuture<>();
        Function<ByteBufferPool, LimitingStringReadChannelListener> stringReadChannelListener = (bufferPool) ->
            new LimitingStringReadChannelListener(bufferPool, maxDownloadSize) {
                @Override
                protected void stringDone(StringResult result) {
                    onResponseCompletedFuture.complete(result);
                }
                @Override
                protected void error(IOException e) {
                    onResponseCompletedFuture.completeExceptionally(e);
                }
        };

        Response response = new Response();

        CompletableFuture.completedFuture(null).thenCompose(nul -> {
                undertowClient.connect(clientConnection, uri, xnioWorker, buffer, DEFAULT_OPTIONS);
                return clientConnectionFuture;
            }
        ).thenCompose(connection -> {
                ClientRequest request = new ClientRequest();
                request.setMethod(HttpString.tryFromString(requestMethod));
                request.setPath(uri.getPath());
                request.getRequestHeaders().put(Headers.HOST, uri.getHost());
                request.getRequestHeaders().put(Headers.TRANSFER_ENCODING, "chunked");
                requestHeaders.forEach((k,v) -> request.getRequestHeaders().put(new HttpString(k), v));
                connection.sendRequest(request, onRequestStart);
                return onRequestStartFuture;
            }
        ).thenCompose(exchange -> {
            new ByteBufferWriteChannelListener(data).setup(exchange.getRequestChannel());
            exchange.setResponseListener(onResponseStarted);
                return onResponseStartedFuture;
            }
        ).thenCompose(exchange -> {
                response.code = exchange.getResponse().getResponseCode();
                if (response.getCode() == 503) { //retry if 503 Service Unavailable
                    onResponseCompletedFuture.completeExceptionally(
                            new RuntimeException("Received response code: " + response.getCode()));
                } else {
                    stringReadChannelListener.apply(buffer).setup(exchange.getResponseChannel());
                }
                return onResponseCompletedFuture;
            }
        ).handle((stringResult, throwable) -> {
            if (throwable != null) {
                logger.error("Error: ", throwable);
                if (maxRetries > 0 && attempt < maxRetries) {
                    executor.schedule(
                            () -> invokeAttempt(
                                    uri,
                                    requestMethod,
                                    requestHeaders,
                                    data,
                                    responseFuture,
                                    attempt + 1,
                                    maxRetries,
                                    waitBeforeRetry,
                                    maxDownloadSize),
                            waitBeforeRetry * attempt, TimeUnit.MILLISECONDS
                    );
                } else {
                    logger.info("Invocation completed exceptionally. Response code: " + response.getCode());
                    responseFuture.completeExceptionally(throwable);
                }
            } else {
                response.stringResult = stringResult;
                logger.info("Invocation completed. Response code: " + response.getCode());
                responseFuture.complete(response);
            }
            return null;
        });
    }

    @Override
    public void close() throws IOException {
        logger.info("Shutting down.");
        executor.shutdownNow();
        xnioWorker.shutdown();
    }

    public static class Response {
        private int code;
        private StringResult stringResult;

        public Response() {
        }

        public Response(int code, StringResult stringResult) {
            this.code = code;
            this.stringResult = stringResult;
        }

        public int getCode() {
            return code;
        }

        public StringResult getStringResult() {
            return stringResult;
        }
    }
}
