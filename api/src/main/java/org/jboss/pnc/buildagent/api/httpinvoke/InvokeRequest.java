package org.jboss.pnc.buildagent.api.httpinvoke;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.net.URL;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@JsonDeserialize(builder = InvokeRequest.Builder.class)
public class InvokeRequest {

    private final String command;

    private final URL callbackUrl;

    private final String callbackMethod;

    public InvokeRequest(String command, URL callbackUrl, String callbackMethod) {
        this.command = command;
        this.callbackUrl = callbackUrl;
        this.callbackMethod = callbackMethod;
    }

    private InvokeRequest(Builder builder) {
        command = builder.command;
        callbackUrl = builder.callbackUrl;
        callbackMethod = builder.callbackMethod;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getCommand() {
        return command;
    }

    public URL getCallbackUrl() {
        return callbackUrl;
    }

    public String getCallbackMethod() {
        return callbackMethod;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private String command;

        private URL callbackUrl;

        private String callbackMethod;

        private Builder() {
        }

        public Builder command(String command) {
            this.command = command;
            return this;
        }

        public Builder callbackUrl(URL callbackUrl) {
            this.callbackUrl = callbackUrl;
            return this;
        }

        public Builder callbackMethod(String callbackMethod) {
            this.callbackMethod = callbackMethod;
            return this;
        }

        public InvokeRequest build() {
            return new InvokeRequest(this);
        }
    }
}
