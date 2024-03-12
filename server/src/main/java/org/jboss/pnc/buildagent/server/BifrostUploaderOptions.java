/**
 * JBoss, Home of Professional Open Source.
 * Copyright 2021 Red Hat, Inc., and individual contributors
 * as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jboss.pnc.buildagent.server;

import java.nio.file.Path;
import java.util.Map;

public class BifrostUploaderOptions {
    private final String bifrostURL;
    private Path logPath;
    private final int maxRetries;
    private final int waitBeforeRetry;
    private final Map<String, String> mdc;

    public BifrostUploaderOptions(String bifrostURL, int maxRetries, int waitBeforeRetry, Map<String, String> mdc) {
        this.bifrostURL = bifrostURL;
        this.maxRetries = maxRetries;
        this.waitBeforeRetry = waitBeforeRetry;
        this.mdc = mdc;
    }

    public void setLogPath(Path logPath) {
        this.logPath = logPath;
    }

    public String getBifrostURL() {
        return bifrostURL;
    }

    public Path getLogPath() {
        return logPath;
    }

    public int getMaxRetries() {
        return maxRetries;
    }

    public int getWaitBeforeRetry() {
        return waitBeforeRetry;
    }

    public Map<String, String> getMdc() {
        return mdc;
    }
}
