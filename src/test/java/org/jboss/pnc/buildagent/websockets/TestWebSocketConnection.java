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

import io.termd.core.pty.Status;
import org.jboss.pnc.buildagent.MockProcess;
import org.jboss.pnc.buildagent.TermdServer;
import org.jboss.pnc.buildagent.spi.TaskStatusUpdateEvent;
import org.jboss.pnc.buildagent.util.ObjectWrapper;
import org.jboss.pnc.buildagent.util.Wait;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static org.jboss.pnc.buildagent.websockets.Client.WEB_SOCKET_LISTENER_PATH;
import static org.jboss.pnc.buildagent.websockets.Client.WEB_SOCKET_TERMINAL_PATH;


/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class TestWebSocketConnection {

    private static final Logger log = LoggerFactory.getLogger(TestWebSocketConnection.class);

    private static final String HOST = "localhost";
    private static final int PORT = TermdServer.getNextPort();
    private static final String TEST_COMMAND = "java -cp ./target/test-classes/ org.jboss.pnc.buildagent.MockProcess 4";

    private File logFolder = Paths.get("").toAbsolutePath().toFile();

    String terminalUrl = "http://" + HOST + ":" + PORT + WEB_SOCKET_TERMINAL_PATH;
    String listenerUrl = "http://" + HOST + ":" + PORT + WEB_SOCKET_LISTENER_PATH;

    @BeforeClass
    public static void setUP() throws Exception {
        TermdServer.startServer(HOST, PORT);
    }

    @AfterClass
    public static void tearDown() {
        TermdServer.stopServer();
    }

    @Test
    public void serverShouldBeUpAndRunning() throws Exception {
        String content = readUrl(HOST, PORT, "/");
        Assert.assertTrue("Cannot read response from serverThread.", content.length() > 0);
    }

    @Test
    public void clientShouldBeAbleToRunRemoteCommandAndReceiveResults() throws Exception {

        List<TaskStatusUpdateEvent> remoteResponseStatuses = new ArrayList<>();
        Consumer<TaskStatusUpdateEvent> onStatusUpdate = (statusUpdateEvent) -> {
            remoteResponseStatuses.add(statusUpdateEvent);
        };
        Client statusListenerClient = Client.connectStatusListenerClient(listenerUrl, onStatusUpdate);

        List<String> remoteResponses = new ArrayList<>();

        Consumer<String> onResponseData = (responseData) -> {
            remoteResponses.add(responseData);
        };
        Client commandExecutingClient = Client.connectCommandExecutingClient(terminalUrl, TEST_COMMAND, Optional.of(onResponseData));

        assertThatResultWasReceived(remoteResponses, 5, ChronoUnit.SECONDS);
        assertThatCommandCompletedSuccessfully(remoteResponseStatuses, 5, ChronoUnit.SECONDS);
        assertThatLogWasWritten(remoteResponseStatuses);

        commandExecutingClient.close();
        statusListenerClient.close();
    }

    @Test
    public void shouldExecuteTwoTasksAndWriteToLogs() throws Exception {

        ObjectWrapper<Boolean> completed = new ObjectWrapper<>(false);
        Consumer<TaskStatusUpdateEvent> onStatusUpdate = (statusUpdateEvent) -> {
            if (statusUpdateEvent.getNewStatus().equals(Status.COMPLETED)) {
                assertTestCommandOutputIsWrittenToLog(statusUpdateEvent.getTaskId());
                completed.set(true);
            }
        };
        Client statusListenerClient = Client.connectStatusListenerClient(listenerUrl, onStatusUpdate);

        Client commandExecutingClient = Client.connectCommandExecutingClient(terminalUrl, TEST_COMMAND, Optional.empty());
        Wait.forCondition(() -> completed.get(), 5, ChronoUnit.SECONDS);
        completed.set(false);

        Client.executeRemoteCommand(commandExecutingClient, TEST_COMMAND);
        Wait.forCondition(() -> completed.get(), 5, ChronoUnit.SECONDS);
        completed.set(false);

        commandExecutingClient.close();
        statusListenerClient.close();
    }

    private void assertThatResultWasReceived(List<String> strings, long timeout, TemporalUnit timeUnit) throws InterruptedException {
        Supplier<Boolean> evaluationSupplier = () -> {
            List<String> stringsCopy = new ArrayList<>(strings);
            String remoteResponses = stringsCopy.stream().collect(Collectors.joining());
            return remoteResponses.contains(MockProcess.WELCOME_MESSAGE);
        };

        try {
            Wait.forCondition(evaluationSupplier, timeout, timeUnit);
        } catch (TimeoutException e) {
            throw new AssertionError("Response should contain message " + MockProcess.WELCOME_MESSAGE + ".", e);
        }
    }

    private void assertThatCommandCompletedSuccessfully(List<TaskStatusUpdateEvent> remoteResponseStatuses, long timeout, TemporalUnit timeUnit) throws InterruptedException {
        Supplier<Boolean> evaluationSupplier = () -> {
            List<TaskStatusUpdateEvent> receivedStatuses = remoteResponseStatuses;
            List<Status> collectedUpdates = receivedStatuses.stream().map(event -> event.getNewStatus()).collect(Collectors.toList());
            return collectedUpdates.contains(Status.RUNNING) && collectedUpdates.contains(Status.COMPLETED);
        };

        try {
            Wait.forCondition(evaluationSupplier, timeout, timeUnit);
        } catch (TimeoutException e) {
            throw new AssertionError("Response should contain status Status.RUNNING and Status.COMPLETED.", e);
        }
    }

    private void assertThatLogWasWritten(List<TaskStatusUpdateEvent> remoteResponseStatuses) throws IOException {
        List<TaskStatusUpdateEvent> responses = remoteResponseStatuses;
        Optional<TaskStatusUpdateEvent> firstResponse = responses.stream().findFirst();
        if (!firstResponse.isPresent()) {
            throw new AssertionError("There is no status update event to retrieve task id.");
        }

        TaskStatusUpdateEvent taskStatusUpdateEvent = firstResponse.get();
        String taskId = taskStatusUpdateEvent.getTaskId() + "";

        assertTestCommandOutputIsWrittenToLog(taskId);
    }

    private void assertTestCommandOutputIsWrittenToLog(String taskId) {
        File logFile = new File(logFolder, "console-" + taskId + ".log");
        Assert.assertTrue("Missing log file: " + logFile, logFile.exists());

        String fileContent = null;
        try {
            fileContent = new String(Files.readAllBytes(logFile.toPath()));
        } catch (IOException e) {
            throw new AssertionError("Cannot read log file.", e);
        }
        Assert.assertTrue("Missing executed command in log file.", fileContent.contains(TEST_COMMAND));
        Assert.assertTrue("Missing response message in log file.", fileContent.contains("Hello again"));
        Assert.assertTrue("Incomplete log file.", fileContent.contains("I'm done."));
        Assert.assertTrue("Missing or invalid completion state.", fileContent.contains("# Finished with status: " + Status.COMPLETED.toString()));
    }

    private String readUrl(String host, int port, String path) throws IOException {
        URL url = new URL("http://" + host + ":" + port + path);
        URLConnection connection = url.openConnection();
        connection.connect();
        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        String inputLine;
        StringBuilder stringBuilder = new StringBuilder();
        while ((inputLine = bufferedReader.readLine()) != null) {
            stringBuilder.append(inputLine);
        }
        bufferedReader.close();
        return stringBuilder.toString();
    }

}
