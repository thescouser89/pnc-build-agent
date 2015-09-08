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

import io.termd.core.pty.PtyMaster;
import io.termd.core.pty.Status;
import io.termd.core.pty.TtyBridge;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.ServerSocket;
import java.nio.charset.Charset;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class BuildAgent {

    Logger log = LoggerFactory.getLogger(BuildAgent.class);

    private final List<PtyMaster> runningTasks = new ArrayList<>();

    private final Set<Consumer<TaskStatusUpdateEvent>> statusUpdateListeners = new HashSet<>();
    private Optional<Path> logFolder;
    private Charset charset = Charset.forName("UTF-8");
    private UndertowBootstrap undertowBootstrap;

    private String host;
    private int port;

    public boolean addStatusUpdateListener(Consumer<TaskStatusUpdateEvent> statusUpdateListener) {
        return statusUpdateListeners.add(statusUpdateListener);
    }

    public boolean removeStatusUpdateListener(Consumer<TaskStatusUpdateEvent> statusUpdateListener) {
        return statusUpdateListeners.remove(statusUpdateListener);
    }

    public void start(String host, int portCandidate, String contextPath, Optional<Path> logFolder, final Runnable onStart) throws InterruptedException, BuildAgentException {
        if(portCandidate == 0) {
            portCandidate = findFirstFreePort();
        }
        this.port = portCandidate;
        this.host = host;
        this.logFolder = logFolder;

        undertowBootstrap = new UndertowBootstrap(host, port, contextPath, this, runningTasks);

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
            ptyMaster.setChangeHandler(onTaskStatusUpdate(ptyMaster));
            runningTasks.add(ptyMaster);
            undertowBootstrap.getTerminalSession().addTask(ptyMaster);
        };
    }

    public void newTtyConnection(WebSocketTtyConnection conn) {
        TtyBridge ttyBridge = new TtyBridge(conn)
                .setProcessListener(onTaskCreated())
                .setProcessStdinListener(undertowBootstrap.getTerminalSession().getProcessInputConsumer())
                .setProcessStdoutListener(undertowBootstrap.getTerminalSession().getProcessOutputConsumer());
        ttyBridge.readline();
    }

    private BiConsumer<Status, Status> onTaskStatusUpdate(PtyMaster ptyMaster) {
        return (oldStatus, newStatus) -> {
            switch (newStatus) {
                case COMPLETED:
                case FAILED:
                case INTERRUPTED:
                    removeCompletedTask(ptyMaster);

            }
            notifyStatusUpdated(new TaskStatusUpdateEvent(ptyMaster.getId() + "", oldStatus, newStatus));
        };
    }

    private void removeCompletedTask(PtyMaster task) {
        undertowBootstrap.getTerminalSession().removeTask(task);
        runningTasks.remove(task);
    }

    void notifyStatusUpdated(TaskStatusUpdateEvent statusUpdateEvent) {
        for (Consumer<TaskStatusUpdateEvent> statusUpdateListener : statusUpdateListeners) {
            log.debug("Notifying listener {} in task {} with new status {}", statusUpdateListener, statusUpdateEvent.getTaskId(), statusUpdateEvent.getNewStatus());
            statusUpdateListener.accept(statusUpdateEvent);
        }
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

    public Optional<Path> getLogFolder() {
        return logFolder;
    }

}
