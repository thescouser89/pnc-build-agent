package org.jboss.pnc.buildagent.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.pnc.buildagent.api.Constants;
import org.jboss.pnc.buildagent.api.httpinvoke.Cancel;
import org.jboss.pnc.buildagent.api.httpinvoke.InvokeRequest;
import org.jboss.pnc.buildagent.api.httpinvoke.InvokeResponse;
import org.jboss.pnc.buildagent.common.http.HttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class BuildAgentHttpClient {
    private final Logger logger = LoggerFactory.getLogger(BuildAgentHttpClient.class);
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private final URL invokerUrl;

    private final URL callbackUrl;

    private final String callbackMethod;

    private String sessionId;

    public BuildAgentHttpClient(String termBaseUrl, URL callbackUrl, String callbackMethod)
            throws BuildAgentClientException {
        this.callbackUrl = callbackUrl;
        this.callbackMethod = callbackMethod;
        try {
            invokerUrl = new URL(termBaseUrl + Constants.HTTP_INVOKER_FULL_PATH);
        } catch (MalformedURLException e) {
            throw new BuildAgentClientException("Invalid term url.", e);
        }
        try {
            httpClient = new HttpClient();
        } catch (IOException e) {
            throw new BuildAgentClientException("Cannot initialize http client.", e);
        }
    }

    public void executeCommand(String command)
            throws BuildAgentClientException, InterruptedException, ExecutionException, TimeoutException {
        InvokeRequest request = new InvokeRequest(command, callbackUrl, callbackMethod);
        CompletableFuture<HttpClient.Response> responseFuture = new CompletableFuture<>();
        try {
            String requestJson = objectMapper.writeValueAsString(request);
            httpClient.invoke(invokerUrl.toURI(), "POST", requestJson, responseFuture);
        } catch (JsonProcessingException e) {
            throw new BuildAgentClientException("Cannot serialize request.", e);
        } catch (URISyntaxException e) {
            throw new BuildAgentClientException("Invalid command execution url.", e);
        }
        HttpClient.Response response = responseFuture.get(5, TimeUnit.SECONDS); //TODO timeout
        InvokeResponse invokeResponse;
        try {
            invokeResponse = objectMapper.readValue(response.getString(), InvokeResponse.class);
        } catch (IOException e) {
            throw new BuildAgentClientException("Cannot read command invocation response.", e);
        }
        sessionId = invokeResponse.getSessionId();
    }

    /**
     *
     * @return true if cancel was successful
     * @throws BuildAgentClientException
     */
    public boolean cancel() throws BuildAgentClientException {
        Cancel request = new Cancel(sessionId);
        CompletableFuture<HttpClient.Response> responseFuture = new CompletableFuture<>();
        try {
            String requestJson = objectMapper.writeValueAsString(request);
            httpClient.invoke(invokerUrl.toURI(), "PUT", requestJson, responseFuture);
        } catch (JsonProcessingException e) {
            throw new BuildAgentClientException("Cannot serialize cancel request.", e);
        } catch (IOException e) {
            throw new BuildAgentClientException("Cannot invoke cancel.", e);
        } catch (URISyntaxException e) {
            throw new BuildAgentClientException("Invalid cancel url.", e);
        }
        try {
            HttpClient.Response response = responseFuture.get(5, TimeUnit.SECONDS);
            return response.getCode() == 200;
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            throw new BuildAgentClientException("Error reading cancel request.", e);
        }
    }

    public String getSessionId() {
        return sessionId;
    }
}
