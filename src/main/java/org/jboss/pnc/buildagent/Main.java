package org.jboss.pnc.buildagent;

import io.termd.core.http.vertx.NativeProcessBootstrap;
import io.termd.core.http.vertx.SockJSBootstrap;

import java.util.concurrent.CountDownLatch;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class Main {
    public static void main(String[] args) {
        String host = "localhost";
        int port = 8080;

        Runnable onStart = () -> {
            System.out.println("Server started on " + host + ":" + port);
        };
        new Main(host, port, onStart);
    }

    public Main(final String host, final int port, Runnable onStart) {
        SockJSBootstrap bootstrap = new SockJSBootstrap(
                host,
                port,
                new NativeProcessBootstrap());
        final CountDownLatch latch = new CountDownLatch(1);
        bootstrap.bootstrap(event -> {
            if (event.succeeded()) {
                onStart.run();
            } else {
                System.out.println("Could not start");
                event.cause().printStackTrace();
                latch.countDown();
            }
        });
        try {
            latch.await();
        } catch (InterruptedException e) {
            e.printStackTrace(); //TODO
        }
    }
}
