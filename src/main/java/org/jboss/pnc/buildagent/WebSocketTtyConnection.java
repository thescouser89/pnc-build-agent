package org.jboss.pnc.buildagent;

import io.termd.core.http.TtyConnectionBridge;
import io.termd.core.util.Handler;
import io.undertow.websockets.core.AbstractReceiveListener;
import io.undertow.websockets.core.BufferedBinaryMessage;
import io.undertow.websockets.core.WebSocketChannel;
import io.undertow.websockets.core.WebSockets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xnio.ChannelListener;
import org.xnio.Pooled;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.Executor;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class WebSocketTtyConnection {

  private static Logger log = LoggerFactory.getLogger(WebSocketTtyConnection.class);

  private  final TtyConnectionBridge ttyConnection;

  public WebSocketTtyConnection(final WebSocketChannel webSocketChannel, Executor executor) {

    Handler<byte[]> onByteHandler = (bytes) -> WebSockets.sendBinary(ByteBuffer.wrap(bytes), webSocketChannel, null);
    ttyConnection = new TtyConnectionBridge(onByteHandler, executor);

    registerWebSocketChannelListener(webSocketChannel);
    webSocketChannel.resumeReceives();
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
          String msg = new String(byteBuffer.array());
          log.trace("Sending message to decoder: {}", msg);
          ttyConnection.writeToDecoder(msg);
        } finally {
          pulledData.discard();
        }
      }
    };
    webSocketChannel.getReceiveSetter().set(listener);
  }

  public TtyConnectionBridge getTtyConnection() {
    return ttyConnection;
  }
}
