package org.jboss.pnc.buildagent.server;

import io.undertow.servlet.api.InstanceFactory;
import io.undertow.servlet.api.InstanceHandle;
import io.undertow.servlet.util.ImmediateInstanceHandle;
import org.jboss.pnc.buildagent.common.http.HttpClient;
import org.jboss.pnc.buildagent.server.httpinvoker.SessionRegistry;
import org.jboss.pnc.buildagent.server.servlet.HttpInvoker;

import java.security.NoSuchAlgorithmException;
import java.util.Set;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class HttpInvokerFactory implements InstanceFactory<HttpInvoker> {

    private Set<ReadOnlyChannel> readOnlyChannels;

    private SessionRegistry sessionRegistry;

    private HttpClient httpClient;

    public HttpInvokerFactory(Set<ReadOnlyChannel> readOnlyChannels, HttpClient httpClient, SessionRegistry sessionRegistry) {
        this.readOnlyChannels = readOnlyChannels;
        this.httpClient = httpClient;
        this.sessionRegistry = sessionRegistry;
    }

    @Override
    public InstanceHandle<HttpInvoker> createInstance() throws InstantiationException {
        try {
            return new ImmediateInstanceHandle<>(new HttpInvoker(readOnlyChannels, sessionRegistry, httpClient));
        } catch (NoSuchAlgorithmException e) {
            throw new InstantiationException("Cannot create HttpInvoker: " + e.getMessage());
        }
    }
}
