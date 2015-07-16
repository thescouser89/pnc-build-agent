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

package org.jboss.pnc.buildagent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.termd.core.pty.Status;
import org.jboss.pnc.buildagent.util.ObjectWrapper;
import org.jboss.pnc.buildagent.util.Wait;
import org.jboss.pnc.buildagent.websockets.Client;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static org.jboss.pnc.buildagent.websockets.Client.WEB_SOCKET_LISTENER_PATH;
import static org.jboss.pnc.buildagent.websockets.Client.WEB_SOCKET_TERMINAL_PATH;


/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class TestGetRunningProcesses {

    private static final String HOST = "localhost";
    private static final int PORT = TermdServer.getNextPort();

    private static Logger log = LoggerFactory.getLogger(TestGetRunningProcesses.class);

    private static final String TEST_COMMAND = "java -cp ./target/test-classes/ org.jboss.pnc.buildagent.MockProcess 1 500";

    @BeforeClass
    public static void setUP() throws Exception {
        TermdServer.startServer(HOST, PORT);
    }

    @AfterClass
    public static void tearDown() {
        TermdServer.stopServer();
    }

    @Test
    public void getRunningProcesses() throws Exception {
        String terminalUrl = "http://" + HOST + ":" + PORT + WEB_SOCKET_TERMINAL_PATH;
        String listenerUrl = "http://" + HOST + ":" + PORT + WEB_SOCKET_LISTENER_PATH;

        HttpURLConnection connection = retrieveProcessList();
        Assert.assertEquals(connection.getResponseMessage(), 200, connection.getResponseCode());

        JsonNode node = readResponse(connection);
        Assert.assertEquals(0, node.size());

        String context = this.getClass().getName() + ".getRunningProcesses";

        ObjectWrapper<Boolean> resultReceived = new ObjectWrapper<>(false);
        Consumer<TaskStatusUpdateEvent> onStatusUpdate = (statusUpdateEvent) -> {
            if (statusUpdateEvent.getNewStatus().equals(Status.RUNNING) && context.equals(statusUpdateEvent.getContext())) {
                try {
                    HttpURLConnection afterExecution = retrieveProcessList();
                    Assert.assertEquals(afterExecution.getResponseMessage(), 200, afterExecution.getResponseCode());
                    JsonNode nodeAfterExecution = readResponse(afterExecution);
                    Assert.assertEquals(1, nodeAfterExecution.size());
                    resultReceived.set(true);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        };
        Client eventClient = Client.connectStatusListenerClient(listenerUrl, onStatusUpdate, context);

        Consumer<String> onResponseData = (response) -> {};
        Client commandExecutingClient = Client.connectCommandExecutingClient(terminalUrl, Optional.of(onResponseData), context, Optional.empty());
        Client.executeRemoteCommand(commandExecutingClient, TEST_COMMAND);

        Supplier<Boolean> evaluationSupplier = () -> resultReceived.get();
        Wait.forCondition(evaluationSupplier, 3, ChronoUnit.SECONDS, "Client was not connected within given timeout.");

    }

    private JsonNode readResponse(HttpURLConnection connection) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readTree(connection.getInputStream());
    }

    private HttpURLConnection retrieveProcessList() throws IOException {
        URL url = new URL("http://" + HOST + ":" + PORT + "/processes");
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");
        connection.setDoOutput(true);
        connection.setDoInput(true);
        return connection;
    }

}