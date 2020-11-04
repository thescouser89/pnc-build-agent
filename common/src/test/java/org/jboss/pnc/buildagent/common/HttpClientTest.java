package org.jboss.pnc.buildagent.common;

import org.jboss.pnc.buildagent.common.http.HttpClient;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

public class HttpClientTest {

    @Test @Ignore //not a real test case, inspect the log
    public void shouldRetryFailedConnection()
            throws IOException, URISyntaxException, ExecutionException, InterruptedException {
        HttpClient httpClient = new HttpClient();
        CompletableFuture<HttpClient.Response> completableFuture = new CompletableFuture<>();
        httpClient.invoke(new URI("http://host-not-found/"), "GET", "", completableFuture);
        completableFuture.get();
    }
}
