package org.jboss.pnc.buildagent.server.servlet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.termd.core.pty.PtyMaster;
import io.termd.core.pty.Status;
import org.jboss.pnc.api.dto.HeartbeatConfig;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.buildagent.api.TaskStatusUpdateEvent;
import org.jboss.pnc.buildagent.api.httpinvoke.Cancel;
import org.jboss.pnc.buildagent.api.httpinvoke.InvokeRequest;
import org.jboss.pnc.buildagent.api.httpinvoke.InvokeResponse;
import org.jboss.pnc.buildagent.api.httpinvoke.RetryConfig;
import org.jboss.pnc.buildagent.common.Arrays;
import org.jboss.pnc.buildagent.common.http.HttpClient;
import org.jboss.pnc.buildagent.common.security.Md5;
import org.jboss.pnc.buildagent.server.ReadOnlyChannel;
import org.jboss.pnc.buildagent.server.httpinvoker.CommandSession;
import org.jboss.pnc.buildagent.server.httpinvoker.Heartbeat;
import org.jboss.pnc.buildagent.server.httpinvoker.SessionRegistry;
import org.jboss.pnc.buildagent.server.termserver.StatusConverter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class HttpInvoker extends HttpServlet {

    private final Logger logger = LoggerFactory.getLogger(HttpInvoker.class);

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Set<ReadOnlyChannel> readOnlyChannels;

    private final SessionRegistry sessionRegistry;

    private final HttpClient httpClient;

    private final RetryConfig retryConfig;
    private final Heartbeat heartbeat;

    private final Md5 stdoutChecksum;


    public HttpInvoker(
            Set<ReadOnlyChannel> readOnlyChannels,
            SessionRegistry sessionRegistry,
            HttpClient httpClient,
            RetryConfig retryConfig,
            Heartbeat heartbeat)
            throws NoSuchAlgorithmException {
        this.readOnlyChannels = readOnlyChannels;
        this.sessionRegistry = sessionRegistry;
        this.httpClient = httpClient;
        this.retryConfig = retryConfig;
        this.heartbeat = heartbeat;
        this.stdoutChecksum = new Md5();
    }

    @Override
    protected void doPut(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        Cancel cancelRequest = objectMapper.readValue(request.getInputStream(), Cancel.class);
        Optional<CommandSession> commandSession = sessionRegistry.get(cancelRequest.getSessionId());

        if (commandSession.isPresent()) {
            PtyMaster ptyMaster = commandSession.get().getPtyMaster();
            ptyMaster.interruptProcess(); //onComplete is called when the process is interrupted
            response.setStatus(200);
        } else {
            response.setStatus(204);
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String requestString = request.getReader().lines().collect(Collectors.joining());
        logger.info("Received request body: {}.", requestString);
        InvokeRequest invokeRequest = objectMapper.readValue(requestString, InvokeRequest.class);

        String command = invokeRequest.getCommand();

        CommandSession commandSession = new CommandSession(readOnlyChannels);
        String sessionId = commandSession.getSessionId();

        HeartbeatConfig heartbeatConfig = invokeRequest.getHeartbeatConfig();
        Optional<Future<?>> heartbeatFuture;
        if (heartbeatConfig != null) {
            heartbeatFuture = Optional.of(heartbeat.start(heartbeatConfig));
        } else {
            heartbeatFuture = Optional.empty();
        }

        PtyMaster ptyMaster = new PtyMaster(command, stdOut -> handleOutput(commandSession, stdOut), (nul) -> {});
        ptyMaster.setChangeHandler((oldStatus, newStatus) -> {
            if (newStatus.isFinal()) {
                onComplete(commandSession, newStatus, invokeRequest.getCallback());
                heartbeatFuture.ifPresent(future -> this.heartbeat.stop(future));
            }
        });
        commandSession.setPtyMaster(ptyMaster);
        sessionRegistry.put(commandSession);

        ptyMaster.start();

        //write response
        InvokeResponse invokeResponse = new InvokeResponse(sessionId);
        response.getWriter().write(objectMapper.writeValueAsString(invokeResponse));
    }

    private void handleOutput(CommandSession commandSession, int[] stdOut) {
        byte[] buffer = Arrays.charIntstoBytes(stdOut, StandardCharsets.UTF_8);
        stdoutChecksum.add(buffer);
        commandSession.handleOutput(buffer);
    }

    private void onComplete(CommandSession commandSession, Status newStatus, Request callback) {
        TaskStatusUpdateEvent.Builder updateEventBuilder = TaskStatusUpdateEvent.newBuilder();
        updateEventBuilder.context(callback.getAttachment());
        try {
            String digest = stdoutChecksum.digest();
            commandSession.close();
            updateEventBuilder
                    .taskId(commandSession.getSessionId())
                    .newStatus(StatusConverter.fromTermdStatus(newStatus))
                    .outputChecksum(digest);
        } catch (IOException e) {
            updateEventBuilder
                    .taskId(commandSession.getSessionId())
                    .newStatus(org.jboss.pnc.buildagent.api.Status.SYSTEM_ERROR)
                    .message("Unable to flush stdout: " + e.getMessage());
        }

        //notify completion via callback
        try {
            String data = objectMapper.writeValueAsString(updateEventBuilder.build());
            httpClient.invoke(
                    callback,
                    ByteBuffer.wrap(data.getBytes(UTF_8)),
                    retryConfig.getMaxRetries(),
                    retryConfig.getWaitBeforeRetry(),
                    -1L,
                    0,
                    0)
            .handle((response, throwable) -> {
                if (throwable != null) {
                    logger.error("Cannot send completion callback.", throwable);
                } else {
                    logger.info("Completion callback sent. Response code: {}.", response.getCode());
                }
                return null;
            });
        } catch (JsonProcessingException e) {
            logger.error("Cannot serialize invoke object.", e);
        }
    }
}
