package org.jboss.pnc.buildagent.common;

import io.undertow.Undertow;
import io.undertow.server.DefaultByteBufferPool;
import io.undertow.server.HttpHandler;
import org.jboss.pnc.buildagent.common.http.HttpClient;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.xnio.IoUtils;
import org.xnio.OptionMap;
import org.xnio.Options;
import org.xnio.Xnio;
import org.xnio.XnioWorker;
import org.xnio.channels.StreamSinkChannel;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import static org.jboss.pnc.buildagent.common.http.HttpClient.DEFAULT_OPTIONS;

public class HttpClientTest {

    @Test @Ignore //not a real test case, inspect the log
    public void shouldRetryFailedConnection()
            throws IOException, URISyntaxException, ExecutionException, InterruptedException {
        HttpClient httpClient = new HttpClient();
        CompletableFuture<HttpClient.Response> completableFuture = httpClient.invoke(new URI("http://host-not-found/"), "GET", "");
        completableFuture.get();
    }

    @Test//(timeout = 5000L)
    public void shouldLimitDownloadSize()
            throws IOException, URISyntaxException, ExecutionException, InterruptedException {
        HttpHandler handler = exchange -> {
            StreamSinkChannel responseChannel = exchange.getResponseChannel();
            for (int i = 0; i < 1000; i++) {
                responseChannel.write(ByteBuffer.wrap(UUID.randomUUID().toString().getBytes()));
            }
            responseChannel.close();
        };
        Undertow undertow = Undertow.builder()
                .addHttpListener(8080, "localhost")
                .setHandler(handler)
                .build();
        undertow.start();

        HttpClient httpClient = null;
        try {
            final Xnio xnio = Xnio.getInstance();
            XnioWorker xnioWorker = xnio.createWorker(null, DEFAULT_OPTIONS);

            DefaultByteBufferPool buffer = new DefaultByteBufferPool(false, 1024, 10, 10, 100);

            httpClient = new HttpClient(xnioWorker, buffer);
            CompletableFuture<HttpClient.Response> responseFuture = httpClient.invoke(new URI("http://localhost:8080/"),
                    "GET",
                    Collections.emptySet(),
                    ByteBuffer.allocate(0),
                    0,
                    0,
                    1024);
            HttpClient.Response response = responseFuture.get();
            Assert.assertFalse("Should be uncompleted response.", response.getStringResult().isComplete());
            Assert.assertTrue("Download limit exceeded." ,response.getStringResult().getString().length() < 2 * 1024); //limit + buffer size
        } finally {
            undertow.stop();
            IoUtils.safeClose(httpClient);
        }
    }

    Undertow server;

    @Test @Ignore //TODO set up timeouts
    public void shouldFailWhenRemoteServerDoesNotRespond()
            throws IOException, URISyntaxException, ExecutionException, InterruptedException {

        HttpHandler handler = exchange -> {
            StreamSinkChannel responseChannel = exchange.getResponseChannel();
            Thread.sleep(1000L);
            System.out.println("Stopping ...");
            server.stop();
            System.out.println("Stopped!");
            for (int i = 0; i < 5; i++) {
                System.out.println("Writing ...");
                responseChannel.write(ByteBuffer.wrap(UUID.randomUUID().toString().getBytes()));
                Thread.sleep(1000L);
            }
            responseChannel.writeFinal(ByteBuffer.wrap(UUID.randomUUID().toString().getBytes()));
        };
        Undertow.Builder builder = Undertow.builder().addHttpListener(8080, "localhost").setHandler(handler);
        server = builder.build();
        server.start();

        HttpClient httpClient = null;
        try {
            final Xnio xnio = Xnio.getInstance();

            OptionMap options = OptionMap.builder()
                    .set(Options.WORKER_IO_THREADS, 8)
                    .set(Options.TCP_NODELAY, true)
                    .set(Options.KEEP_ALIVE, true)
                    .set(Options.WORKER_NAME, "Build Agent Http Client")
                    .set(Options.READ_TIMEOUT, 30000)
                    .set(Options.WRITE_TIMEOUT, 30000)
                    .getMap();

            XnioWorker xnioWorker = xnio.createWorker(null, options);

            DefaultByteBufferPool buffer = new DefaultByteBufferPool(false, 1024, 10, 10, 100);

            httpClient = new HttpClient(xnioWorker, buffer);
            CompletableFuture<HttpClient.Response> responseFuture = httpClient.invoke(new URI("http://localhost:8080/"),
                    "GET",
                    Collections.emptySet(),
                    ByteBuffer.allocate(0),
                    0,
                    0,
                    -1L);
            HttpClient.Response response = responseFuture.get();
            System.out.println("Done: " + response.getCode() + " - " + response.getStringResult().getString());
        } finally {
            server.stop();
            IoUtils.safeClose(httpClient);
        }
    }

}
