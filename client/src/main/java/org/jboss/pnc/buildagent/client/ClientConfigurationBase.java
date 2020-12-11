package org.jboss.pnc.buildagent.client;

import org.jboss.pnc.buildagent.api.httpinvoke.RetryConfig;

/**
 * @author <a href="mailto:matejonnet@gmail.opecom">Matej Lazar</a>
 */
public abstract class ClientConfigurationBase {

    protected String termBaseUrl;

    /**
     * Liveness response timeout in milliseconds.
     */
    protected long livenessResponseTimeout;

    protected RetryConfig retryConfig;

    public String getTermBaseUrl() {
        return termBaseUrl;
    }

    public long getLivenessResponseTimeout() {
        return livenessResponseTimeout;
    }

    public RetryConfig getRetryConfig() {
        return retryConfig;
    }
}
