/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016 Red Hat, Inc., and individual contributors
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

import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.StandardCharsets;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class ReadOnlyWebSocketTextChannel implements ReadOnlyChannel {

    private Logger log = LoggerFactory.getLogger(ReadOnlyWebSocketTextChannel.class);

    private WebSocketChannel webSocketChannel;
    private StringBuilder stringBuilder;

    public ReadOnlyWebSocketTextChannel(WebSocketChannel webSocketChannel) {
        this.webSocketChannel = webSocketChannel;
        stringBuilder = new StringBuilder();
    }

    @Override
    public void writeOutput(byte[] buffer) {
        String string = new String(buffer, StandardCharsets.UTF_8);
        stringBuilder.append(string);
        log.trace("sending string [{}], raw [{}]", string, buffer);
        if (string.equals("\n") || string.equals("\r\n")) {
            WebSockets.sendText(stringBuilder.toString(), webSocketChannel, null);
            stringBuilder = new StringBuilder();
        }
    }
}
