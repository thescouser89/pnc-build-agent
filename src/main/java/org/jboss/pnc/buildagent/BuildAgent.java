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

import io.termd.core.pty.PtyBootstrap;
import io.termd.core.pty.PtyMaster;
import io.termd.core.pty.PtyStatusEvent;
import io.termd.core.pty.Status;
import io.termd.core.tty.TtyConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class BuildAgent {

    Logger log = LoggerFactory.getLogger(BuildAgent.class);

    PtyBootstrap ptyBootstrap;

    private final List<PtyMaster> runningTasks = new ArrayList<>();

    private final Set<Consumer<PtyStatusEvent>> statusUpdateListeners = new HashSet<>();
    private Optional<Path> logFolder;
    private Charset charset = Charset.forName("UTF-8");
    private UndertowBootstrap undertowBootstrap;

    private String host;
    private int port;

    public boolean addStatusUpdateListener(Consumer<PtyStatusEvent> statusUpdateListener) {
        return statusUpdateListeners.add(statusUpdateListener);
    }

    public boolean removeStatusUpdateListener(Consumer<PtyStatusEvent> statusUpdateListener) {
        return statusUpdateListeners.remove(statusUpdateListener);
    }

    public void start(String host, int portCandidate, Optional<Path> logFolder, final Runnable onStart) throws InterruptedException, BuildAgentException {
        if(portCandidate == 0) {
            portCandidate = findFirstFreePort();
        }
        this.port = portCandidate;
        this.host = host;
        this.logFolder = logFolder;

        ptyBootstrap = new PtyBootstrap(onTaskCreated());

        undertowBootstrap = new UndertowBootstrap(host, port, this, runningTasks);

        undertowBootstrap.bootstrap(new Consumer<Boolean>() {
            @Override
            public void accept(Boolean event) {
                if (event) {
                    System.out.println("Server started on " + host + ":" + port);
                    if (onStart != null)
                        onStart.run();
                } else {
                    System.out.println("Could not start");
                }
            }
        });
    }

    private int findFirstFreePort() {
        try (ServerSocket s = new ServerSocket(0)) {
            return s.getLocalPort();
        } catch (IOException e) {
            throw new IllegalArgumentException("Could not obtain default port, try specifying it explicitly");
        }
    }

    private Consumer<PtyMaster> onTaskCreated() {
        return (ptyMaster) -> {
            Optional<FileOutputStream> fileOutputStream = Optional.empty();
            if (logFolder.isPresent()) {
                try {
                    Path logPath = logFolder.get().resolve("console-" + ptyMaster.getId() + ".log");

                    log.info("Opening log file {}.", logPath);
                    FileOutputStream stream = new FileOutputStream(logPath.toFile(), true);
                    fileOutputStream = Optional.of(stream);
                    registerProcessLogger(stream, ptyMaster);
                } catch (IOException e) {
                    log.error("Cannot open fileChannel: ", e);
                }
            }
            ptyMaster.setTaskStatusUpdateListener(onTaskStatusUpdate(fileOutputStream));
            runningTasks.add(ptyMaster);
        };
    }

    private Consumer<PtyStatusEvent> onTaskStatusUpdate(Optional<FileOutputStream> fileOutputStream) {
        return (taskStatusUpdateEvent) -> {
            fileOutputStream.ifPresent((fos) -> taskStatusUpdateLogger(fos, taskStatusUpdateEvent));
            notifyStatusUpdated(taskStatusUpdateEvent);
        };
    }

    private void taskStatusUpdateLogger(FileOutputStream fileOutputStream, PtyStatusEvent taskStatusUpdateEvent) {
        PtyMaster task = taskStatusUpdateEvent.getProcess();
        Status newStatus = taskStatusUpdateEvent.getNewStatus();
        switch (newStatus) {
            case COMPLETED:
            case FAILED:
            case INTERRUPTED:
                runningTasks.remove(task);
                try {
                    String completed = "% # Finished with status: " + newStatus + "\r\n";
                    fileOutputStream.write(completed.getBytes(charset));
                    fileOutputStream.close();
                } catch (IOException e) {
                    log.error("Cannot close log file channel: ", e);
                }
        }
    }

    private void registerProcessLogger(FileOutputStream fileOutputStream, PtyMaster task) throws IOException {
        Consumer<String> processInputConsumer = (line) -> {
            try {
                String command = "% " + line + "\r\n";
                fileOutputStream.write(command.getBytes(charset));
            } catch (IOException e) {
                log.error("Cannot write command line of task " + task.getId() + " to file.", e);
            }
        };

        Consumer<int[]> processOutputConsumer = (ints) -> {
            DataOutputStream out = new DataOutputStream(fileOutputStream);
            for (int anInt : ints) {
                try {
                    out.write(anInt);
                } catch (IOException e) {
                    log.error("Cannot write task " + task.getId() + " output to file.", e);
                }
            }
        };

        task.setProcessInputConsumer(processInputConsumer);
        task.setProcessOutputConsumer(processOutputConsumer);
    }

    void notifyStatusUpdated(PtyStatusEvent statusUpdateEvent) {
        for (Consumer<PtyStatusEvent> statusUpdateListener : statusUpdateListeners) {
            log.debug("Notifying listener {} status update {}", statusUpdateListener, statusUpdateEvent);
            statusUpdateListener.accept(statusUpdateEvent);
        }
    }

    public Consumer<TtyConnection> getPtyBootstrap() {
        return ptyBootstrap;
    }

    public void stop() {
        undertowBootstrap.stop();
        System.out.println("Server stopped");
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }
}
