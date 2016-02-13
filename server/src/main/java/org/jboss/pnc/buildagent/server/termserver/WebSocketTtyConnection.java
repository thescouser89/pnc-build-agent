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

package org.jboss.pnc.buildagent.server.termserver;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.termd.core.io.BinaryDecoder;
import io.termd.core.io.BinaryEncoder;
import io.termd.core.tty.TtyConnection;
import io.termd.core.tty.TtyEvent;
import io.termd.core.tty.TtyEventDecoder;
import io.termd.core.tty.TtyOutputMode;
import io.termd.core.util.Vector;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedBinaryMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import org.jboss.pnc.buildagent.BuildAgentException;
import org.jboss.pnc.buildagent.api.ResponseMode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.Pooled;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class WebSocketTtyConnection implements TtyConnection {

    private Charset charset;
    private Vector size;
    private Consumer<Vector> sizeHandler;
    private final TtyEventDecoder eventDecoder;
    private final BinaryDecoder decoder;
    private final Consumer<int[]> stdout;
    private Consumer<Void> closeHandler;
    private Consumer<String> termHandler;
    private long lastAccessedTime = System.currentTimeMillis();


    private static Logger log = LoggerFactory.getLogger(WebSocketTtyConnection.class);
    private WebSocketChannel webSocketChannel;
    private ResponseMode responseMode;
    private final ScheduledExecutorService executor;
    private Set<ReadOnlyChannel> readonlyChannels = new HashSet<>();

    public WebSocketTtyConnection(ScheduledExecutorService executor) {
        this.charset = StandardCharsets.UTF_8;
        this.size = new Vector(Integer.MAX_VALUE, Integer.MAX_VALUE);
        this.eventDecoder = new TtyEventDecoder(3, 26, 4);
        this.decoder = new BinaryDecoder(512, charset, eventDecoder);
        this.stdout = new TtyOutputMode(new BinaryEncoder(charset, this::write));
        this.executor = executor;
    }

    private void write(byte[] buffer) {
        if (isOpen()) {
            if (ResponseMode.TEXT.equals(responseMode)) {
                WebSockets.sendText(new String(buffer, StandardCharsets.UTF_8), webSocketChannel, null);
            } else {
                WebSockets.sendBinary(ByteBuffer.wrap(buffer), webSocketChannel, null);
            }
        }
        readonlyChannels.forEach((channel) -> channel.writeOutput(buffer));
    }

    @Override
    public void execute(Runnable task) {
        executor.execute(task);
    }

    @Override
    public void schedule(Runnable task, long delay, TimeUnit unit) {
        executor.schedule(task, delay, unit);
    }

    private void registerWebSocketChannelListener(WebSocketChannel webSocketChannel) {
        ChannelListener<WebSocketChannel> listener = new AbstractReceiveListener() {

            @Override
            protected void onFullBinaryMessage(WebSocketChannel channel, BufferedBinaryMessage message) throws IOException {
                log.trace("Server received full binary message");
                Pooled<ByteBuffer[]> pulledData = message.getData();
                try {
                    ByteBuffer[] resource = pulledData.getResource();
                    ByteBuffer byteBuffer = WebSockets.mergeBuffers(resource);
                    writeToDecoder(byteBuffer);
                } catch (BuildAgentException e) {
                    log.error("Invalid request received.", e);
                } finally {
                    pulledData.discard();
                }
            }
        };
        webSocketChannel.getReceiveSetter().set(listener);
    }

    public void writeToDecoder(ByteBuffer byteBuffer) throws BuildAgentException {
        if (byteBuffer.capacity() == 1) { //handle events
            decoder.write(byteBuffer.array());
        } else {
            String msg = new String(byteBuffer.array());

            ObjectMapper mapper = new ObjectMapper();
            Map<String, Object> obj;
            String action;
            try {
                obj = mapper.readValue(msg, Map.class);
                action = (String) obj.get("action");
            } catch (IOException e) {
                log.error("Cannot write to decoder.", e);
                return;
            }
            if (action != null) {
                switch (action) {
                    case "read":
                        lastAccessedTime = System.currentTimeMillis();
                        Object data = obj.get("data");
                        String dataStr;
                        try {
                            dataStr = (String) data;
                        } catch (ClassCastException e) {
                            throw new BuildAgentException("String value expected.", e);
                        }
                        decoder.write(dataStr.getBytes());
                        break;
                    case "resize":
                        try {
                            int cols = (int) obj.getOrDefault("cols", size.x());
                            int rows = (int) obj.getOrDefault("rows", size.y());
                            if (cols > 0 && rows > 0) {
                                Vector newSize = new Vector(cols, rows);
                                if (!newSize.equals(size())) {
                                    size = newSize;
                                    if (sizeHandler != null) {
                                        sizeHandler.accept(size);
                                    }
                                }
                            }
                        } catch (Exception e) {
                            // Invalid size
                            // Log this
                        }
                        break;
                }
            }
        }
    }


    public boolean isOpen() {
        return webSocketChannel != null && webSocketChannel.isOpen();
    }

    public void setWebSocketChannel(WebSocketChannel webSocketChannel, ResponseMode responseMode) {
        this.webSocketChannel = webSocketChannel;
        this.responseMode = responseMode;
        registerWebSocketChannelListener(webSocketChannel);
        webSocketChannel.resumeReceives();
    }

    public void addReadonlyChannel(ReadOnlyChannel webSocketChannel) {
        readonlyChannels.add(webSocketChannel);
    }

    public void removeReadonlyChannel(ReadOnlyChannel webSocketChannel) {
        readonlyChannels.remove(webSocketChannel);
    }

    public void removeWebSocketChannel() {
        webSocketChannel = null;
    }

    @Override
    public long lastAccessedTime() {
        return lastAccessedTime;
    }

    @Override
    public Vector size() {
        return size;
    }

    @Override
    public Charset inputCharset() {
        return charset;
    }

    @Override
    public Charset outputCharset() {
        return charset;
    }

    @Override
    public String terminalType() {
        return "vt100";
    }

    @Override
    public Consumer<String> getTerminalTypeHandler() {
        return termHandler;
    }

    @Override
    public void setTerminalTypeHandler(Consumer<String> handler) {
        termHandler = handler;
    }

    @Override
    public Consumer<Vector> getSizeHandler() {
        return sizeHandler;
    }

    @Override
    public void setSizeHandler(Consumer<Vector> handler) {
        this.sizeHandler = handler;
    }

    @Override
    public BiConsumer<TtyEvent, Integer> getEventHandler() {
        return eventDecoder.getEventHandler();
    }

    @Override
    public void setEventHandler(BiConsumer<TtyEvent, Integer> handler) {
        eventDecoder.setEventHandler(handler);
    }

    @Override
    public Consumer<int[]> getStdinHandler() {
        return eventDecoder.getReadHandler();
    }

    @Override
    public void setStdinHandler(Consumer<int[]> handler) {
        eventDecoder.setReadHandler(handler);
    }

    @Override
    public Consumer<int[]> stdoutHandler() {
        return stdout;
    }

    @Override
    public void setCloseHandler(Consumer<Void> closeHandler) {
        this.closeHandler = closeHandler;
    }

    @Override
    public Consumer<Void> getCloseHandler() {
        return closeHandler;
    }

    @Override
    public void close() {
        Consumer<Void> closeHandler = getCloseHandler();
        if (closeHandler != null) {
            closeHandler.accept(null);
        }
    }
}
