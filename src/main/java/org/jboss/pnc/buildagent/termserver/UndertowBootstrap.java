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

package org.jboss.pnc.buildagent.termserver;

import io.undertow.Undertow;
import io.undertow.server.HttpServerExchange;
import io.undertow.websockets.core.WebSocketChannel;
import org.jboss.pnc.buildagent.BuildAgentException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;


/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class UndertowBootstrap {

  Logger log = LoggerFactory.getLogger(UndertowBootstrap.class);

  final String host;
  final int port;
  private Undertow server;
  final ConcurrentHashMap<String, Term> terms = new ConcurrentHashMap<>();
  private ScheduledExecutorService executor;
  private Optional<WebSocketChannel> additionalReadOnlyChannel;

  public UndertowBootstrap(String host, int port, ScheduledExecutorService executor, Optional<WebSocketChannel> additionalReadOnlyChannel) {
    this.host = host;
    this.port = port;
    this.executor = executor;
    this.additionalReadOnlyChannel = additionalReadOnlyChannel;
  }

  public void bootstrap(final Consumer<Boolean> completionHandler) {
    server = Undertow.builder()
        .addHttpListener(port, host)
        .setHandler((exchange) -> handleWebSocketRequests(exchange, Configurations.TERM_PATH, Configurations.PROCESS_UPDATES_PATH))
        .build();

    server.start();

    completionHandler.accept(true);
  }

  protected void handleWebSocketRequests(HttpServerExchange exchange, String termPath, String processUpdatePath) throws Exception {
    String requestPath = exchange.getRequestPath();
    if (requestPath.startsWith(termPath)) {
      log.debug("Connecting to term ...");
      String invokerContext = requestPath.replace(termPath + "/", "");
      Term term = getTerm(invokerContext);
      term.getWebSocketHandler(additionalReadOnlyChannel).handleRequest(exchange);
    } else  if (requestPath.startsWith(processUpdatePath)) {
      log.debug("Connecting status listener ...");
      String invokerContext = requestPath.replace(processUpdatePath + "/", "");
      Term term = getTerm(invokerContext);
      term.webSocketStatusUpdateHandler().handleRequest(exchange);
    }
  }

  private Term getTerm(String invokerContext) {
    return terms.computeIfAbsent(invokerContext, ctx -> createNewTerm(invokerContext));
  }

  protected Term createNewTerm(String invokerContext) {
    log.debug("Creating new term for context [{}].", invokerContext);
    Runnable onDestroy = () -> terms.remove(invokerContext);
    Term term = new Term(invokerContext, onDestroy, executor);

    return term;
  }

  public void stop() {
    if (server != null) {
      server.stop();
    }
  }

  public String getHost() {
    return host;
  }

  public int getPort() {
    return port;
  }

  public Map<String, Term> getTerms() {
    Map termsClone = new HashMap<>();
    termsClone.putAll(terms);
    return termsClone;
  }
}
