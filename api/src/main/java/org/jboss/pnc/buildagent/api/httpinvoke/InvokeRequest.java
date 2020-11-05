package org.jboss.pnc.buildagent.api.httpinvoke;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.net.URL;
import java.util.Collections;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@JsonDeserialize(builder = InvokeRequest.Builder.class)
public class InvokeRequest {

    private final String command;

    private final Request request;

    /**
     * @deprecated use {@link InvokeRequest(String, Request )}
     */
    @Deprecated
    public InvokeRequest(String command, URL callbackUrl, String callbackMethod) {
        this.command = command;
        this.request = new Request(callbackMethod, callbackUrl, Collections.emptyMap());
    }

    public InvokeRequest(String command, Request request) {
        this.command = command;
        this.request = request;
    }

    private InvokeRequest(Builder builder) {
        command = builder.command;
        request = builder.request;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getCommand() {
        return command;
    }

    public Request getRequest() {
        return request;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private String command;

        private Request request;

        private Builder() {
        }

        public Builder command(String command) {
            this.command = command;
            return this;
        }

        public Builder request(Request request) {
            this.request = request;
            return this;
        }

        public InvokeRequest build() {
            return new InvokeRequest(this);
        }
    }
}
