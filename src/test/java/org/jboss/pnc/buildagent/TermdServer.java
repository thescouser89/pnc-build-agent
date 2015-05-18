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
    static void startServer(String host, int port) throws InterruptedException {
        Semaphore mutex = new Semaphore(1);
        Runnable onStart = () ->  {
            mutex.release();
        };
        mutex.acquire();
        serverThread = new Thread(() -> new Main(host, port, onStart), "termd-serverThread-thread");
        serverThread.start();

        mutex.acquire();
    }

    static void stopServer() {
        serverThread.interrupt();
    }
}
