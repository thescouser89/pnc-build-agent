package org.jboss.pnc.buildagent.server;

import java.io.IOException;
import java.util.Set;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class ChannelJoin implements ReadOnlyChannel {

    private Set<ReadOnlyChannel> channels;

    public ChannelJoin(Set<ReadOnlyChannel> channels) {
        this.channels = channels;
    }

    @Override
    public void writeOutput(byte[] buffer) {
        for (ReadOnlyChannel channel : channels) {
            channel.writeOutput(buffer);
        }
    }

    @Override
    public void close() throws IOException {
        for (ReadOnlyChannel channel : channels) {
            channel.close();
        }
    }
}
