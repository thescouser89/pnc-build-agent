package org.jboss.pnc.buildagent.common.http;

import org.jboss.pnc.api.dto.Request;

import java.util.List;

public interface HeartbeatHttpHeaderProvider {
    List<Request.Header> getHeaders();
}
