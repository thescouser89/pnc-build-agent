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

import io.termd.core.pty.PtyMaster;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class TerminalSession {

    private static final Logger log = LoggerFactory.getLogger(TerminalSession.class);

    private String id;

    Optional<TerminalSessionIoLogger> terminalSessionIoLogger = Optional.empty();

    private Set<WebSocketChannel> channelListeners = new HashSet<>();

    private Set<Consumer<String>> processInputConsumers = new HashSet<>();
    private Set<Consumer<int[]>> processOutputConsumers = new HashSet<>();

    Consumer<String> processInputConsumer = (line) -> {
        String command = "% " + line + "\r\n";
        processInputConsumers.forEach(consumer -> consumer.accept(line));
    };

    Consumer<int[]> processOutputConsumer = (ints) -> {
        processOutputConsumers.forEach(consumer -> consumer.accept(ints));
    };
    private Set<PtyMaster> tasks = new HashSet<>();

    public TerminalSession(Optional<Path> logPath) {
        id = UUID.randomUUID().toString();
        logPath.ifPresent(path -> {
            TerminalSessionIoLogger ioLogger = new TerminalSessionIoLogger(path);
            terminalSessionIoLogger = Optional.of(ioLogger);
            processInputConsumers.add(ioLogger.getInputLogger()); //TODO remove
            processOutputConsumers.add(ioLogger.getOutputLogger());
        });
    }

    public String getId() {
        return id;
    }

    public void addListener(WebSocketChannel webSocketChannel) {
        channelListeners.add(webSocketChannel);
    }

    public void removeListener(WebSocketChannel webSocketChannel) {
        channelListeners.remove(webSocketChannel);
    }

    public void onTtyByte(byte[] buffer) {
        channelListeners.forEach(webSocketChannel -> WebSockets.sendBinary(ByteBuffer.wrap(buffer), webSocketChannel, null));
    }

    public Consumer<String> getProcessInputConsumer() {
        return processInputConsumer;
    }

    public Consumer<int[]> getProcessOutputConsumer() {
        return processOutputConsumer;
    }

    public void addTask(PtyMaster task) {
        tasks.add(task);
    }

    public void removeTask(PtyMaster task) {
        tasks.remove(task);
        if (tasks.isEmpty()) {
            terminalSessionIoLogger.ifPresent(ioLogger -> {
                finalizeLog(ioLogger, task);
                //ioLogger.close(); //TODO close
            });
        }
    }

    private void finalizeLog(TerminalSessionIoLogger ioLogger, PtyMaster task) {
        String completed = "% # Finished with status: " + task.getStatus() + "\r\n";
        ioLogger.write(completed);
    }
}
