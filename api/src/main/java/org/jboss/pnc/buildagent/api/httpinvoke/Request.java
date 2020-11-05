package org.jboss.pnc.buildagent.api.httpinvoke;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.net.URL;
import java.util.Map;

@JsonDeserialize(builder = Request.Builder.class)
public class Request {
    private final String method;
    private final URL url;
    private final Map<String, String> headers;

    public Request(String method, URL url, Map<String, String> headers) {
        this.method = method;
        this.url = url;
        this.headers = headers;
    }

    public String getMethod() {
        return method;
    }

    public URL getUrl() {
        return url;
    }

    public Map<String, String> getHeaders() {
        return headers;
    }


    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {
        private String method;
        private URL url;
        private Map<String, String> headers;

        private Builder() {
        }

        public Builder method(String method) {
            this.method = method;
            return this;
        }

        public Builder url(URL url) {
            this.url = url;
            return this;
        }

        public Builder headers(Map<String, String> headers) {
            this.headers = headers;
            return this;
        }

        public Request build() {
            return new Request(method, url, headers);
        }
    }
}
