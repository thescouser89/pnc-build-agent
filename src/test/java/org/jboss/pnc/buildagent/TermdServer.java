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

import java.util.concurrent.Semaphore;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class TermdServer {

    private static Thread serverThread;

    /**
     * Try to start the build agent and block until it is up and running.
     *
     * @return
     * @throws InterruptedException
     * @param host
     * @param port
     */
    public static void startServer(String host, int port) throws InterruptedException {
        Semaphore mutex = new Semaphore(1);
        Runnable onStart = () ->  {
            System.out.println("Server started."); //TODO log
            mutex.release();
        };
        mutex.acquire();
        serverThread = new Thread(() -> {
            try {
                new Main().start(host, port, onStart);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }, "termd-serverThread-thread");
        serverThread.start();

        mutex.acquire();
    }

    public static void stopServer() {
        System.out.println("Stopping server..."); //TODO log
        serverThread.interrupt();
    }
}
