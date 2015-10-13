/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.pnc.buildagent.websockets;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.jboss.pnc.buildagent.termserver.TaskStatusUpdateEvent;
import org.jboss.pnc.buildagent.util.ObjectWrapper;
import org.jboss.pnc.buildagent.util.Wait;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.websocket.CloseReason;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import java.io.Closeable;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;

/**
 * @see "https://github.com/undertow-io/undertow/blob/5bdddf327209a4abf18792e78148863686c26e9b/websockets-jsr/src/test/java/io/undertow/websockets/jsr/test/BinaryEndpointTest.java"
 *
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class BuildAgentClient implements Closeable {

    private static final Logger log = LoggerFactory.getLogger(BuildAgentClient.class);

    Client statusUpdatesClient;
    Client commandExecutingClient;

    public BuildAgentClient(String termSocketUrl, String statusUpdatesSocketUrl,
            Optional<Consumer<String>> responseDataConsumer,
            Consumer<TaskStatusUpdateEvent> onStatusUpdate,
            String context,
            Optional<String> sessionId) throws TimeoutException, InterruptedException {

        Consumer<TaskStatusUpdateEvent> onStatusUpdateInternal = (event) -> {
            onStatusUpdate.accept(event);
        };

        statusUpdatesClient = connectStatusListenerClient(statusUpdatesSocketUrl, onStatusUpdateInternal, ""); //TODO use context
        commandExecutingClient = connectCommandExecutingClient(termSocketUrl, responseDataConsumer, "", sessionId);
    }

    public void executeCommand(String command) {
        log.info("Executing remote command ...");
        RemoteEndpoint.Basic remoteEndpoint = commandExecutingClient.getRemoteEndpoint();
        String data = "{\"action\":\"read\",\"data\":\"" + command + "\\r\\n\"}";
        try {
            remoteEndpoint.sendBinary(ByteBuffer.wrap(data.getBytes()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static Client connectStatusListenerClient(String webSocketUrl, Consumer<TaskStatusUpdateEvent> onStatusUpdate, String context) {
        Client client = initializeDefault();
        Consumer<String> responseConsumer = (text) -> {
            log.trace("Decoding response: {}", text);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonObject = null;
            try {
                jsonObject = mapper.readTree(text);
            } catch (IOException e) {
                log.error( "Cannot read JSON string: " + text, e);
            }
            try {
                TaskStatusUpdateEvent taskStatusUpdateEvent = TaskStatusUpdateEvent.fromJson(jsonObject.get("event").toString());
                onStatusUpdate.accept(taskStatusUpdateEvent);
            } catch (IOException e) {
                log.error("Cannot deserialize TaskStatusUpdateEvent.", e);
            }
        };
        client.onStringMessage(responseConsumer);

        client.onClose(closeReason -> {
        });

        try {
            client.connect(webSocketUrl + "/?context=" + context);
        } catch (Exception e) {
            throw new AssertionError("Failed to connect to remote client.", e);
        }
        return client;
    }

    private static Client connectCommandExecutingClient(String webSocketUrl, Optional<Consumer<String>> responseDataConsumer, String context, Optional<String> sessionId) throws InterruptedException, TimeoutException {
        ObjectWrapper<Boolean> connected = new ObjectWrapper<>(false);

        Client client = initializeDefault();

        Consumer<byte[]> responseConsumer = (bytes) -> {
            String responseData = new String(bytes);
            log.trace("Checking for command line 'ready'(%) marker...");
            if ("% ".equals(responseData)) { //TODO use events
                connected.set(true);
            } else {
                responseDataConsumer.ifPresent((rdc) -> rdc.accept(responseData));;
            }
        };
        client.onBinaryMessage(responseConsumer);

        client.onClose(closeReason -> {
        });

        String sessionIdParam = "";
        if (sessionId.isPresent()) sessionIdParam = "&sessionId=" + sessionId;

        try {
            client.connect(webSocketUrl + "/?context=" + context + sessionIdParam);
        } catch (Exception e) {
            throw new AssertionError("Failed to connect to remote client.", e);
        }
        //if reconnecting (sessionId present) return immediately
        Wait.forCondition(() -> sessionId.isPresent() || connected.get(), 10, ChronoUnit.SECONDS, "Client was not connected within given timeout.");
        return client;
    }

    private static Client initializeDefault() {
        Client client = new Client();

        Consumer<Session> onOpen = (session) -> {
            log.info("Client connection opened.");
        };

        Consumer<CloseReason> onClose = (closeReason) -> {
            log.info("Client connection closed. " + closeReason);
        };

        client.onOpen(onOpen);
        client.onClose(onClose);

        return client;
    }

    @Override
    public void close() throws IOException {
        try {
            commandExecutingClient.close();
            statusUpdatesClient.close();
        } catch (Exception e) {
            log.error("Cannot close client.", e);
        }
    }
}
