package org.jboss.pnc.buildagent.common.http;

public class HttpClientConfiguration {
    private int maxRetries;
    private int waitBeforeRetry;

    public int getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(int maxRetries) {
        this.maxRetries = maxRetries;
    }

    public int getWaitBeforeRetry() {
        return waitBeforeRetry;
    }

    public void setWaitBeforeRetry(int waitBeforeRetry) {
        this.waitBeforeRetry = waitBeforeRetry;
    }
}
