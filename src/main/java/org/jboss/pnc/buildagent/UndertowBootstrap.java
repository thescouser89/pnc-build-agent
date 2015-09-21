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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.termd.core.pty.PtyMaster;
import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.PathHandler;
import io.undertow.server.handlers.ResponseCodeHandler;
import io.undertow.servlet.api.DeploymentInfo;
import io.undertow.servlet.api.DeploymentManager;
import io.undertow.websockets.WebSocketConnectionCallback;
import io.undertow.websockets.WebSocketProtocolHandshakeHandler;
import io.undertow.websockets.core.CloseMessage;
import io.undertow.websockets.core.WebSockets;
import org.jboss.pnc.buildagent.servlet.Download;
import org.jboss.pnc.buildagent.servlet.Upload;
import org.jboss.pnc.buildagent.servlet.Welcome;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.io.IOException;
import java.net.URL;
import java.util.Collection;
import java.util.Deque;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.jar.Manifest;
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
    private String contextPath;
    final BuildAgent buildAgent;
    private final Executor executor = Executors.newFixedThreadPool(1);
    private final Collection<PtyMaster> runningTasks;
    private Undertow server;
    TerminalSession terminalSession;

    public UndertowBootstrap(String host, int port, String contextPath, BuildAgent buildAgent, Collection runningTasks) {
        this.host = host;
        this.port = port;
        this.contextPath = contextPath;
        this.buildAgent = buildAgent;
        this.runningTasks = runningTasks;
    }

    public void bootstrap(final Consumer<Boolean> completionHandler) throws BuildAgentException {

        String servletPath = contextPath + "/servlet";
        String socketPath = contextPath + "/socket";
        String httpPath = contextPath + "/";

        DeploymentInfo servletBuilder = deployment()
                .setClassLoader(UndertowBootstrap.class.getClassLoader())
                .setContextPath(servletPath)
                .setDeploymentName("ROOT.war")
                .addServlets(
                        servlet("WelcomeServlet", Welcome.class)
                                .addMapping("/")
                                .addMapping("/index*"),
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
            throw new BuildAgentException("Cannot deploy servlets.", e);
        }

        PathHandler pathHandler = Handlers.path()
                .addPrefixPath(servletPath, servletHandler)
                .addPrefixPath(socketPath, exchange -> UndertowBootstrap.this.handleWebSocketRequests(exchange, socketPath))
                .addPrefixPath(httpPath, exchange -> UndertowBootstrap.this.handleHttpRequests(exchange, httpPath));

        server = Undertow.builder()
                .addHttpListener(port, host)
                .setHandler(pathHandler)
                .build();

        server.start();

        completionHandler.accept(true);
    }

    private void handleWebSocketRequests(HttpServerExchange exchange, String socketPath) throws Exception {
        String requestPath = exchange.getRequestPath();

        if (requestPath.startsWith(socketPath + "/term")) {
            Deque<String> context = exchange.getQueryParameters().get("context");
            String invokerContext = "";
            if (context != null) invokerContext = context.getFirst();

            Deque<String> sessionIds = exchange.getQueryParameters().get("sessionId");
            Optional<String> sessionId = Optional.empty();
            if (sessionIds != null) sessionId = Optional.of(sessionIds.getFirst());

            getWebSocketHandler(invokerContext, sessionId).handleRequest(exchange);
            return;
        }
        if (requestPath.startsWith(socketPath + "/process-status-updates")) {
            Deque<String> context = exchange.getQueryParameters().get("context");
            String invokerContext = "";
            if (context != null) invokerContext = context.getFirst();
            webSocketStatusUpdateHandler(invokerContext).handleRequest(exchange);
            return;
        }
    }

    private void handleHttpRequests(HttpServerExchange exchange, String httpPath) throws Exception {
        String requestPath = exchange.getRequestPath();

        if (pathMatches(requestPath, httpPath)) {
            exchange.getResponseSender().send("Welcome to PNC Build Agent (" + getManifestInformation() + ')');
            return;
        }
        if (pathMatches(requestPath, httpPath + "processes")) {
            getProcessStatusHandler().handleRequest(exchange);
            return;
        }
        ResponseCodeHandler.HANDLE_404.handleRequest(exchange);
    }

    private boolean pathMatches(String requestPath, String path) {
        return requestPath.equals(path) || (requestPath + "/").equals(path);
    }

    private HttpHandler getProcessStatusHandler() {
        return exchange -> {
            Map<String, Object> tasksMap = runningTasks.stream().collect(Collectors.toMap(t -> String.valueOf(t.getId()), t -> t.getStatus().toString()));
            ObjectMapper mapper = new ObjectMapper();
            String jsonString = mapper.writeValueAsString(tasksMap);
            exchange.getResponseSender().send(jsonString);
        };
    }

    private HttpHandler getWebSocketHandler(String invokerContext, Optional<String> sessionId) {
        WebSocketConnectionCallback onWebSocketConnected = (exchange, webSocketChannel) -> {
            if (sessionId.isPresent()) {
                //TODO Optional<TerminalSession> sessionCandidate = activeSessions.values().stream().findFirst();
                //if (sessionCandidate.isPresent()) {
                //    sessionCandidate.get().addListener(webSocketChannel);
                //} else {
                //    log.warn("Client is trying to connect to non existing session.");
                //}
                terminalSession.addListener(webSocketChannel);
                log.debug("New socket listener added to an existing session.");
            } else {
                terminalSession = new TerminalSession(buildAgent.getLogFolder());
                //activeSessions.put(terminalSession.getId(), terminalSession); //TODO destroy and remove session when there is no connection and no running task

                WebSocketTtyConnection conn = new WebSocketTtyConnection(webSocketChannel, terminalSession, executor);
                terminalSession.addListener(webSocketChannel);
                buildAgent.newTtyConnection(conn);
                log.debug("New session created and socket listener added to it.");
            }
            webSocketChannel.addCloseTask(channel -> {
                terminalSession.removeListener(webSocketChannel);
            });
        };

        HttpHandler webSocketHandshakeHandler = new WebSocketProtocolHandshakeHandler(onWebSocketConnected);
        return webSocketHandshakeHandler;
    }

    private HttpHandler webSocketStatusUpdateHandler(String invokerContext) {
        WebSocketConnectionCallback webSocketConnectionCallback = (exchange, webSocketChannel) -> {
            Consumer<TaskStatusUpdateEvent> statusUpdateListener = (statusUpdateEvent) -> {
                Map<String, Object> statusUpdate = new HashMap<>();
                statusUpdate.put("action", "status-update");
                TaskStatusUpdateEvent taskStatusUpdateEventWrapper = new TaskStatusUpdateEvent(statusUpdateEvent);
                statusUpdate.put("event", taskStatusUpdateEventWrapper);

                ObjectMapper objectMapper = new ObjectMapper();
                try {
                    String message = objectMapper.writeValueAsString(statusUpdate);
                    WebSockets.sendText(message, webSocketChannel, null);
                } catch (JsonProcessingException e) {
                    log.error("Cannot write object to JSON", e);
                    String errorMessage = "Cannot write object to JSON: " + e.getMessage();
                    WebSockets.sendClose(CloseMessage.UNEXPECTED_ERROR, errorMessage, webSocketChannel, null);
                }
            };
            log.debug("Registering new status update listener {}.", statusUpdateListener);
            buildAgent.addStatusUpdateListener(statusUpdateListener);
            webSocketChannel.addCloseTask((task) -> buildAgent.removeStatusUpdateListener(statusUpdateListener));
        };

        HttpHandler webSocketHandshakeHandler = new WebSocketProtocolHandshakeHandler(webSocketConnectionCallback);
        return webSocketHandshakeHandler;
    }

    public void stop() {
        if(server != null) {
            server.stop();
        }
    }


    private String getManifestInformation() {
        String result = "";
        try {
            final Enumeration<URL> resources = Welcome.class.getClassLoader().getResources("META-INF/MANIFEST.MF");

            while (resources.hasMoreElements()) {
                final URL jarUrl = resources.nextElement();

                log.trace("Processing jar resource " + jarUrl);
                if (jarUrl.getFile().contains("build-agent")) {
                    final Manifest manifest = new Manifest(jarUrl.openStream());
                    result = manifest.getMainAttributes().getValue("Implementation-Version");
                    result += " ( SHA: " + manifest.getMainAttributes().getValue("Scm-Revision") + " ) ";
                    break;
                }
            }
        } catch (final IOException e) {
            log.trace( "Error retrieving information from manifest", e);
        }

        return result;
    }

    public TerminalSession getTerminalSession() {
        return terminalSession;
    }
}
