package org.jboss.pnc.buildagent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.termd.core.http.Task;
import io.termd.core.http.TaskStatusUpdateListener;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.io.Sender;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.WebSocketProtocolHandshakeHandler;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import io.undertow.websockets.spi.WebSocketHttpExchange;
import org.jboss.pnc.buildagent.servlet.Download;
import org.jboss.pnc.buildagent.servlet.Upload;
import org.jboss.pnc.buildagent.servlet.Welcome;
import org.jboss.pnc.buildagent.spi.TaskStatusUpdateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.vertx.java.core.json.JsonObject;

import javax.servlet.ServletException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import static io.undertow.servlet.Servlets.defaultContainer;
import static io.undertow.servlet.Servlets.deployment;
import static io.undertow.servlet.Servlets.servlet;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class UndertowBootstrap {

  Logger log = LoggerFactory.getLogger(UndertowBootstrap.class);

  final String host;
  final int port;
  final Main termdHandler;
  private final Executor executor = Executors.newFixedThreadPool(1);
  private final Collection<Task> runningTasks;

  public UndertowBootstrap(String host, int port, Main termdHandler, Collection runningTasks) {
    this.host = host;
    this.port = port;
    this.termdHandler = termdHandler;
    this.runningTasks = runningTasks;
  }

  public void bootstrap(final Consumer<Boolean> completionHandler) {

    String servletPath = "/";
    String socketPath = "/socket";

    DeploymentInfo servletBuilder = deployment()
        .setClassLoader(UndertowBootstrap.class.getClassLoader())
        .setContextPath(servletPath)
        .setDeploymentName("ROOT.war")
        .addServlets(
            servlet("WelcomeServlet", Welcome.class)
                .addMapping("/")
                .addMapping("/index"),
            servlet("UploaderServlet", Upload.class)
                .addMapping("/upload/*"),
            servlet("DownloaderServlet", Download.class)
                .addMapping("/download/*"));

    DeploymentManager manager = defaultContainer().addDeployment(servletBuilder);
    manager.deploy();

    HttpHandler servletHandler = null;
    try {
      servletHandler = manager.start();
    } catch (ServletException e) {
      e.printStackTrace();//TODO handle exception
    }

    PathHandler pathHandler = Handlers.path(Handlers.redirect(servletPath))
        .addPrefixPath(servletPath, servletHandler)
        .addPrefixPath(socketPath, exchange -> UndertowBootstrap.this.handleRequest(exchange));

    Undertow undertow = Undertow.builder()
      .addHttpListener(port, host)
      .setHandler(pathHandler)
      .build();

    undertow.start();

    completionHandler.accept(true);
  }

  private void handleRequest(HttpServerExchange exchange) throws Exception {
    String requestPath = exchange.getRequestPath();
    Sender responseSender = exchange.getResponseSender();

    if (requestPath.equals("/term")) {
      getWebSocketHandler().handleRequest(exchange);
      return;
    }
    if (requestPath.equals("/process-status-updates")) {
      webSocketStatusUpdateHandler().handleRequest(exchange);
      return;
    }
    if (requestPath.equals("/processes")) {
      getProcessStatusHandler().handleRequest(exchange);
      return;
    }

    if (requestPath.startsWith("/upload/")) {
      getFileUploadHandler().handleRequest(exchange);
      return;
    }

//    try {
//      String resourcePath = "io/termd/core/http" + requestPath; //TODO static file server
//      String content = readResource(resourcePath, this.getClass().getClassLoader());
//      responseSender.send(content);
//    } catch (Exception e) {
//      e.printStackTrace();
//      exchange.setResponseCode(404);
//    } finally {
//      responseSender.close();
//    }
  }

  private HttpHandler getFileUploadHandler() {
    return null;
  }

  private HttpHandler getProcessStatusHandler() {
    return exchange -> {
      Map<String, Object> tasksMap = runningTasks.stream().collect(Collectors.toMap(t -> String.valueOf(t.getId()), t -> t.getStatus().toString()));
      JsonObject jsonObject = new JsonObject(tasksMap);
      exchange.getResponseSender().send(jsonObject.toString());
    };
  }

  private HttpHandler getWebSocketHandler() {
    WebSocketConnectionCallback webSocketConnectionCallback = new WebSocketConnectionCallback() {
      @Override
      public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel webSocketChannel) {
        WebSocketTtyConnection conn = new WebSocketTtyConnection(webSocketChannel, executor);
        termdHandler.getBootstrap().accept(conn.getTtyConnection());
      }
    };

    HttpHandler webSocketHandshakeHandler = new WebSocketProtocolHandshakeHandler(webSocketConnectionCallback);
    return webSocketHandshakeHandler;
  }

  private HttpHandler webSocketStatusUpdateHandler() {
    WebSocketConnectionCallback webSocketConnectionCallback = new WebSocketConnectionCallback() {
      @Override
      public void onConnect(WebSocketHttpExchange exchange, WebSocketChannel webSocketChannel) {
        TaskStatusUpdateListener statusUpdateListener = (statusUpdateEvent) -> {
          Map<String, Object> statusUpdate = new HashMap<>();
          statusUpdate.put("action", "status-update");
          TaskStatusUpdateEvent taskStatusUpdateEventWrapper = new TaskStatusUpdateEvent(statusUpdateEvent);
          statusUpdate.put("event", taskStatusUpdateEventWrapper);

          ObjectMapper objectMapper = new ObjectMapper();
          try {
            WebSockets.sendText(objectMapper.writeValueAsString(statusUpdate), webSocketChannel, null);
          } catch (JsonProcessingException e) {
            e.printStackTrace();//TODO
          }
        };
        log.debug("Registering new status update listener {}.", statusUpdateListener);
        termdHandler.addStatusUpdateListener(statusUpdateListener);
        webSocketChannel.addCloseTask((task) -> termdHandler.removeStatusUpdateListener(statusUpdateListener));
      }
    };

    HttpHandler webSocketHandshakeHandler = new WebSocketProtocolHandshakeHandler(webSocketConnectionCallback);
    return webSocketHandshakeHandler;
  }
}
