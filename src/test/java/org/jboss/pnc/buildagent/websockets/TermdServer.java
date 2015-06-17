package org.jboss.pnc.buildagent.websockets;

import org.jboss.pnc.buildagent.Main;

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

    static void stopServer() {
        System.out.println("Stopping server..."); //TODO log
        serverThread.interrupt();
    }
}
