package org.jboss.pnc.buildagent.client;

import org.jboss.pnc.buildagent.api.httpinvoke.Request;
import org.jboss.pnc.buildagent.api.httpinvoke.RetryConfig;

/**
 * @author <a href="mailto:matejonnet@gmail.opecom">Matej Lazar</a>
 */
public class HttpClientConfiguration extends ClientConfigurationBase {

    private Request callback;
    private RetryConfig retryConfig;

    private HttpClientConfiguration(Builder builder) {
        termBaseUrl = builder.termBaseUrl;
        livenessResponseTimeout = builder.livenessResponseTimeout;
        callback = builder.callback;
        retryConfig = builder.retryConfig;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public static Builder newBuilder(HttpClientConfiguration copy) {
        Builder builder = new Builder();
        builder.termBaseUrl = copy.getTermBaseUrl();
        builder.livenessResponseTimeout = copy.getLivenessResponseTimeout();
        builder.callback = copy.getCallback();
        builder.retryConfig = copy.getRetryConfig();
        return builder;
    }

    public Request getCallback() {
        return callback;
    }

    public RetryConfig getRetryConfig() {
        return retryConfig;
    }

    public static final class Builder {
        private String termBaseUrl;
        private Long livenessResponseTimeout = 30000L;
        private Request callback;
        private RetryConfig retryConfig;

        private Builder() {
        }

        public Builder termBaseUrl(String termBaseUrl) {
            this.termBaseUrl = termBaseUrl;
            return this;
        }

        public Builder livenessResponseTimeout(long livenessResponseTimeout) {
            this.livenessResponseTimeout = livenessResponseTimeout;
            return this;
        }

        public Builder callback(Request callback) {
            this.callback = callback;
            return this;
        }

        public Builder retryConfig(RetryConfig retryConfig) {
            this.retryConfig = retryConfig;
            return this;
        }

        public HttpClientConfiguration build() {
            return new HttpClientConfiguration(this);
        }
    }
}
