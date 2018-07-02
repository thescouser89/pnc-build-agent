package org.jboss.pnc.buildagent.server.termserver;

import io.undertow.websockets.core.WebSocketChannel;
import org.jboss.pnc.buildagent.api.TaskStatusUpdateEvent;

import java.util.function.Consumer; /**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class TaskStatusUpdateListener {

    private final Consumer<TaskStatusUpdateEvent> eventConsumer;

    private final WebSocketChannel webSocketChannel;

    public TaskStatusUpdateListener(Consumer<TaskStatusUpdateEvent> eventConsumer, WebSocketChannel webSocketChannel) {
        this.eventConsumer = eventConsumer;
        this.webSocketChannel = webSocketChannel;
    }

    public Consumer<TaskStatusUpdateEvent> getEventConsumer() {
        return eventConsumer;
    }

    public WebSocketChannel getWebSocketChannel() {
        return webSocketChannel;
    }
}
