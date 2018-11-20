package org.jboss.pnc.buildagent.server;

import java.io.IOException;
import java.net.ServerSocket;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class Options {

    private final String host;
    private final int port;
    private final String bindPath;

    private final boolean socketInvokerEnabled;
    private final boolean httpInvokerEnabled;

    public Options(String host, int bindPort, String bindPath, boolean socketInvokerEnabled, boolean httpInvokerEnabled) {
        this.host = host;
        this.bindPath = bindPath;
        this.socketInvokerEnabled = socketInvokerEnabled;
        this.httpInvokerEnabled = httpInvokerEnabled;

        if (bindPort == 0) {
            port = findFirstFreePort();
        } else {
            port = bindPort;
        }
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getBindPath() {
        return bindPath;
    }

    public boolean isHttpInvokerEnabled() {
        return httpInvokerEnabled;
    }

    public boolean isSocketInvokerEnabled() {
        return socketInvokerEnabled;
    }

    private int findFirstFreePort() {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not obtain default port, try specifying it explicitly");
        }
    }
}
