package org.jboss.pnc.buildagent.common.http;

public class StringResult {
    boolean isComplete;
    String string;

    public StringResult(boolean isComplete, String string) {
        this.isComplete = isComplete;
        this.string = string;
    }

    public String getString() {
        return string;
    }

    public boolean isComplete() {
        return isComplete;
    }
}
