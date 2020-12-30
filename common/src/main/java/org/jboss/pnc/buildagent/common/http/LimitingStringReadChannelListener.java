/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2014 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.jboss.pnc.buildagent.common.http;

import io.undertow.connector.ByteBufferPool;
import io.undertow.connector.PooledByteBuffer;
import io.undertow.server.XnioByteBufferPool;
import io.undertow.websockets.core.UTF8Output;
import org.xnio.ChannelListener;
import org.xnio.IoUtils;
import org.xnio.Pool;
import org.xnio.channels.StreamSourceChannel;

import java.io.IOException;
import java.nio.Buffer;
import java.nio.ByteBuffer;

/**
 * Based on {@link io.undertow.util.StringReadChannelListener}
 *
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public abstract class LimitingStringReadChannelListener implements ChannelListener<StreamSourceChannel> {

    private final UTF8Output string = new UTF8Output();
    private final ByteBufferPool bufferPool;
    private long maxDownloadSize;
    private long read = 0; //already read

    /**
     * @param bufferPool
     * @param maxDownloadSize max bytes to read, -1 for no limit. The result varies for the buffer size, maximum read bytes are readLimit + buffer.size.
     */
    public LimitingStringReadChannelListener(final ByteBufferPool bufferPool, long maxDownloadSize) {
        this.bufferPool = bufferPool;
        this.maxDownloadSize = maxDownloadSize;
    }

    /**
     * @param bufferPool
     * @param maxDownloadSize max bytes to read, -1 for no limit. The result varies for the buffer size, maximum read bytes are readLimit + buffer.size.
     */
    @Deprecated
    public LimitingStringReadChannelListener(final Pool<ByteBuffer> bufferPool, long maxDownloadSize) {
        this.bufferPool = new XnioByteBufferPool(bufferPool);
        this.maxDownloadSize = maxDownloadSize;
    }

    public void setup(final StreamSourceChannel channel) {
        PooledByteBuffer resource = bufferPool.allocate();
        ByteBuffer buffer = resource.getBuffer();
        try {
            int r = 0;
            do {
                r = channel.read(buffer);
                if (r == 0) {
                    channel.getReadSetter().set(this);
                    channel.resumeReads();
                } else if (r == -1) {
                    stringDone(new StringResult(true, string.extract()));
                    IoUtils.safeClose(channel);
                } else {
                    ((Buffer)buffer).flip();
                    string.write(buffer);
                    read +=r;
                    if (maxDownloadSize > -1L && read >= maxDownloadSize) {
                        stringDone(new StringResult(readCompleted(channel), string.extract()));
                        IoUtils.safeClose(channel);
                        break;
                    }
                }
            } while (r > 0);
        } catch (IOException e) {
            error(e);
        } finally {
            resource.close();
        }
    }

    @Override
    public void handleEvent(final StreamSourceChannel channel) {
        PooledByteBuffer resource = bufferPool.allocate();
        ByteBuffer buffer = resource.getBuffer();
        try {
            int r = 0;
            do {
                r = channel.read(buffer);
                if (r == 0) {
                    return;
                } else if (r == -1) {
                    stringDone(new StringResult(true, string.extract()));
                    IoUtils.safeClose(channel);
                } else {
                    buffer.flip();
                    string.write(buffer);
                    read +=r;
                    if (maxDownloadSize > -1L && read >= maxDownloadSize) {
                        stringDone(new StringResult(readCompleted(channel), string.extract()));
                        IoUtils.safeClose(channel);
                        break;
                    }
                }
            } while (r > 0);
        } catch (IOException e) {
            error(e);
        } finally {
            resource.close();
        }
    }

    /**
     * Use only when no further reads from the channel are performed as one byte is lost because of the checking.
     * @return true if there are no more bytes to read.
     */
    private boolean readCompleted(StreamSourceChannel channel) throws IOException {
        return channel.read(ByteBuffer.allocate(1)) == -1;
    }

    protected abstract void stringDone(StringResult stringResult);

    protected abstract void error(IOException e);

}
