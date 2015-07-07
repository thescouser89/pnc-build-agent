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

import java.io.FileNotFoundException;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Scanner;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class TestFileUpload {

    private static final String HOST = "localhost";
    private static final int PORT = 8080;

    private static Logger log = LoggerFactory.getLogger(TestFileUpload.class);

    @BeforeClass
    public static void setUP() throws Exception {
        TermdServer.startServer(HOST, PORT);
    }

    @AfterClass
    public static void tearDown() {
        TermdServer.stopServer();
    }

    @Test
    public void uploadFile() throws Exception {
        Path pwd = Paths.get("").toAbsolutePath();
        Path fileUploadPath = Paths.get(pwd.toString(), "/test-upload.txt");
        URL url = new URL("http://" + HOST + ":" + PORT + "/upload" + fileUploadPath);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("PUT");

        connection.setDoOutput(true);
        connection.setDoInput(true);

        String fileContent = "The quick brown fox jumps over the lazy dog.";
        byte[] fileContentBytes = fileContent.getBytes();
        connection.setRequestProperty("Content-Length", "" + Integer.toString(fileContentBytes.length));

        try (OutputStream outputStream = connection.getOutputStream()) {
            outputStream.write(fileContentBytes);
        }

        Assert.assertEquals(connection.getResponseMessage(), 200, connection.getResponseCode());

        assertFileWasUploaded(fileUploadPath, fileContent);

        fileUploadPath.toFile().delete();
    }

    private void assertFileWasUploaded(Path fileUploadPath, String expectedFileContent) throws FileNotFoundException {
        String actualFileContent = new Scanner(fileUploadPath.toFile()).useDelimiter("\\Z").next();
        log.info("Content written to file: {}", actualFileContent);
        Assert.assertEquals(expectedFileContent, actualFileContent);
    }
}
