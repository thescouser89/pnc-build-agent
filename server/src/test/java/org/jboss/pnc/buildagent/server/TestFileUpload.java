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

package org.jboss.pnc.buildagent.server;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.Scanner;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class TestFileUpload extends FileUploadAbstract {

    private static final String HOST = "localhost";
    private static final int PORT = TermdServer.getNextPort();

    private static Logger log = LoggerFactory.getLogger(TestFileUpload.class);

    @BeforeClass
    public static void setUP() throws Exception {
        TermdServer.startServer(HOST, PORT, "");
    }

    @AfterClass
    public static void tearDown() {
        TermdServer.stopServer();
    }

    @Test
    public void uploadFile() throws Exception {
        super.uploadFile(HOST, PORT, "");
        Thread.sleep(3000);
    }

    private void assertFileWasUploaded(Path fileUploadPath, String expectedFileContent) throws FileNotFoundException {
        String actualFileContent = new Scanner(fileUploadPath.toFile()).useDelimiter("\\Z").next();
        log.info("Content written to file: {}", actualFileContent);
        Assert.assertEquals(expectedFileContent, actualFileContent);
    }
}
