package org.jboss.pnc.buildagent.api.httpinvoke;

public class RetryConfig {

    private int maxRetries;
    private long waitBeforeRetry;

    public RetryConfig(int maxRetries, long waitBeforeRetry) {
        this.maxRetries = maxRetries;
        this.waitBeforeRetry = waitBeforeRetry;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public long getWaitBeforeRetry() {
        return waitBeforeRetry;
    }
}
