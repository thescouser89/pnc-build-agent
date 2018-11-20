package org.jboss.pnc.buildagent.api.httpinvoke;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;
import org.jboss.pnc.buildagent.api.Status;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@JsonDeserialize(builder = Callback.Builder.class)
public class Callback {

    private final String sessionId;

    private Status status;

    private String message;

    public Callback(String sessionId, Status failed, String message) {
        this.sessionId = sessionId;
        status = failed;
        this.message = message;
    }

    public Callback(String sessionId, Status status) {
        this.sessionId = sessionId;
        this.status = status;
    }

    private Callback(Builder builder) {
        sessionId = builder.sessionId;
        status = builder.status;
        message = builder.message;
    }

    public static Builder newBuilder() {
        return new Builder();
    }

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private String sessionId;

        private Status status;

        private String message;

        private Builder() {
        }

        public Builder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }

        public Builder status(Status status) {
            this.status = status;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Callback build() {
            return new Callback(this);
        }
    }
}
