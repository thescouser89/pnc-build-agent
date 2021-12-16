package org.jboss.pnc.buildagent.common.http;

import org.jboss.pnc.api.dto.HeartbeatConfig;
import org.jboss.pnc.api.dto.Request;
import org.jboss.pnc.buildagent.common.concurrent.MDCScheduledThreadPoolExecutor;
import org.jboss.pnc.common.concurrent.NamedThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.ByteBuffer;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;

public class HeartbeatSender {

    private final Logger logger = LoggerFactory.getLogger(HeartbeatSender.class);

    private final ScheduledExecutorService executor;
    private final HttpClient httpClient;

    public HeartbeatSender(HttpClient httpClient) {
        this.httpClient = httpClient;
        executor = new MDCScheduledThreadPoolExecutor(1, new NamedThreadFactory("heartbeat"));
    }

    public Future<?> start(HeartbeatConfig heartbeatConfig) {
        return executor.scheduleAtFixedRate(
                () -> sendHeartbeat(heartbeatConfig.getRequest()),
                0L,
                heartbeatConfig.getDelay(),
                heartbeatConfig.getDelayTimeUnit());
    }

    public void stop(Future<?> heartBeatFuture) {
        heartBeatFuture.cancel(false);
    }

    private void sendHeartbeat(Request heartbeatRequest) {
        httpClient.invoke(heartbeatRequest, ByteBuffer.allocate(0), 0, 0L, -1L, 0, 0)
            .handle((response, throwable) -> {
                if (throwable != null) {
                    logger.error("Cannot send heartbeat.", throwable);
                } else {
                    logger.info("Heartbeat sent.");
                }
                return null;
            });
    }
}
