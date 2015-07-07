/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2015 Red Hat, Inc., and individual contributors
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

package org.jboss.pnc.buildagent;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class TestFileDownload {

    private static final String HOST = "localhost";
    private static final int PORT = TermdServer.getNextPort();

    private static Logger log = LoggerFactory.getLogger(TestFileDownload.class);

    @BeforeClass
    public static void setUP() throws Exception {
        TermdServer.startServer(HOST, PORT);
    }

    @AfterClass
    public static void tearDown() {
        TermdServer.stopServer();
    }

    @Test
    public void downloadFile() throws Exception {
        Path pwd = Paths.get("").toAbsolutePath();
        Path filePath = Paths.get(pwd.toString(), "/test-file.txt");

        String fileContent = "The quick brown fox jumps over the lazy dog.";

        try (PrintWriter out = new PrintWriter(filePath.toFile())) {
            out.write(fileContent);
        }

        URL url = new URL("http://" + HOST + ":" + PORT + "/download" + filePath);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("GET");

        connection.setDoOutput(true);
        connection.setDoInput(true);

        int contentLength = connection.getContentLength();

        byte[] receivedBytes = new byte[contentLength];
        try (InputStream inputStream = connection.getInputStream()) {
            inputStream.read(receivedBytes);
        }

        Assert.assertEquals(connection.getResponseMessage(), 200, connection.getResponseCode());
        String receivedContent = new String(receivedBytes);
        log.info("Received file content: {}", receivedContent);
        Assert.assertEquals(fileContent, receivedContent);

        filePath.toFile().delete();
    }

}
