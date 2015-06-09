package org.jboss.pnc.buildagent.websockets;

import io.termd.core.Status;
import io.termd.core.util.ObjectWrapper;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import javax.websocket.CloseReason;
import javax.websocket.RemoteEndpoint;
import javax.websocket.Session;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.nio.ByteBuffer;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;


/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class UndertowTests {

  private static final Logger log = LoggerFactory.getLogger(UndertowTests.class);

  private static final String HOST = "localhost";
  private static final String WEB_SOCKET_TERMINAL_PATH = "/term";
  private static final String WEB_SOCKET_LISTENER_PATH = "/process-status-updates";
  private static final int PORT = 8080;
  private static final String TEST_COMMAND = "java -help";


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
  public void clientShouldBeAbleToOpenWebSocketConnection() throws Exception {
    String terminalUrl = "http://" + HOST + ":" + PORT + WEB_SOCKET_TERMINAL_PATH;
    String listenerUrl = "http://" + HOST + ":" + PORT + WEB_SOCKET_LISTENER_PATH;

    ObjectWrapper<List<String>> remoteResponseStatusWrapper = new ObjectWrapper<>(new ArrayList<>());
    Client statusListenerClient = connectStatusListenerClient(listenerUrl, remoteResponseStatusWrapper);

    ObjectWrapper<List<String>> remoteResponseWrapper = new ObjectWrapper<>(new ArrayList<>());
    Client commandExecutingClient = connectCommandExecutingClient(terminalUrl, remoteResponseWrapper);

    assertThatResultWasReceived(remoteResponseWrapper, 5, ChronoUnit.SECONDS);
    assertThatCommandCompletedSuccessfully(remoteResponseStatusWrapper, 5, ChronoUnit.SECONDS);

    commandExecutingClient.close();
    statusListenerClient.close();
  }

  private Client connectCommandExecutingClient(String webSocketUrl, ObjectWrapper<List<String>> remoteResponseWrapper) {
    ObjectWrapper<Boolean> testCommandExecuted = new ObjectWrapper<>(false);

    Client client = setUpClient();
    Consumer<byte[]> responseConsumer = (bytes) -> {
      String responseData = new String(bytes);//TODO encoding
//      String response = new String(bytes);
//      JsonObject jsonResponse = new JsonObject(response);
//      String responseAction = jsonResponse.getString("action");
//      if (responseAction == "status") {
//        remoteResponseStatusWrapper.set(jsonResponse.getString("status"));;
//      }
//      String responseData = jsonResponse.getString("data");
      if ("% ".equals(responseData)) {
        if (!testCommandExecuted.get()) {
          testCommandExecuted.set(true);
          executeRemoteCommand(client, TEST_COMMAND);
        }
      } else {
        remoteResponseWrapper.get().add(responseData);
      }
    };
    client.onBinaryMessage(responseConsumer);

    client.onClose(closeReason -> {
    });

    try {
      client.connect(webSocketUrl);
    } catch (Exception e) {
      throw new AssertionError("Failed to connect to remote client.", e);
    }
    return client;
  }

  private Client connectStatusListenerClient(String webSocketUrl, ObjectWrapper<List<String>> remoteResponseStatusWrapper) {
    Client client = setUpClient();
    Consumer<String> responseConsumer = (text) -> {
      remoteResponseStatusWrapper.get().add(text);
    };
    client.onStringMessage(responseConsumer);

    client.onClose(closeReason -> {
    });

    try {
      client.connect(webSocketUrl);
    } catch (Exception e) {
      throw new AssertionError("Failed to connect to remote client.", e);
    }
    return client;
  }

  private void assertThatResultWasReceived(ObjectWrapper<List<String>> remoteResponseWrapper, long timeout, TemporalUnit timeUnit) throws InterruptedException {
    List<String> strings = remoteResponseWrapper.get();

    boolean responseContainsExpectedString = false;
    LocalDateTime stared = LocalDateTime.now();
    while (true) {
      List<String> stringsCopy = new ArrayList<>(strings);
      String remoteResponses = stringsCopy.stream().collect(Collectors.joining());

      if (stared.plus(timeout, timeUnit).isBefore(LocalDateTime.now())) {
        log.info("Remote responses: {}", remoteResponses);
        throw new AssertionError("Did not received expected response in " + timeout + " " + timeUnit);
      }

      if (remoteResponses.contains("-classpath")) {
        responseContainsExpectedString = true;
        log.info("Remote responses: {}", remoteResponses);
        break;
      } else {
        Thread.sleep(200);
      }
    }
    Assert.assertTrue("Response should contain output of " + TEST_COMMAND + ".", responseContainsExpectedString);
  }

  private void assertThatCommandCompletedSuccessfully(ObjectWrapper<List<String>> remoteResponseStatusWrapper, long timeout, TemporalUnit timeUnit) throws InterruptedException {

    boolean responseContainsExpectedStatuses = false;
    LocalDateTime stared = LocalDateTime.now();
    while (true) {
      if (stared.plus(timeout, timeUnit).isBefore(LocalDateTime.now())) {
        log.info("Remote response status: {}", remoteResponseStatusWrapper.get());
        throw new AssertionError("Did not received response status in " + timeout + " " + timeUnit);
      }

      List<String> responses = remoteResponseStatusWrapper.get();
      log.trace("Received status updates: " + responses);
      Set<String> receivedStatuses = new HashSet<>();

      responses.forEach((response) -> {
        log.trace("Decoding response: {}", response);
        JsonObject jsonObject = new JsonObject(response);
        receivedStatuses.add(jsonObject.getObject("event").getString("new-status"));
      });

      if (receivedStatuses.contains(Status.RUNNING.toString()) &&
          receivedStatuses.contains(Status.SUCCESSFULLY_COMPLETED.toString())) {
        responseContainsExpectedStatuses = true;
        break;
      } else {
        Thread.sleep(200);
      }
    }
    Assert.assertTrue("Response should contain status SUCCESS.", responseContainsExpectedStatuses);
  }

  private void executeRemoteCommand(Client client, String command) {
    log.info("Executing remote command ...");
    RemoteEndpoint.Basic remoteEndpoint = client.getRemoteEndpoint();
    String data = "{\"action\":\"read\",\"data\":\"" + command + "\\r\\n\"}";
    try {
      remoteEndpoint.sendBinary(ByteBuffer.wrap(data.getBytes()));
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  private Client setUpClient() {
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
