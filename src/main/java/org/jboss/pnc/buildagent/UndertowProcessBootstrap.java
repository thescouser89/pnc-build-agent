package org.jboss.pnc.buildagent;

import io.termd.core.http.Bootstrap;
import io.termd.core.http.Task;
import io.termd.core.http.TaskStatusUpdateEvent;
import io.termd.core.http.TaskStatusUpdateListener;
import io.termd.core.tty.TtyConnection;
import io.termd.core.util.Handler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class UndertowProcessBootstrap {

  Logger log = LoggerFactory.getLogger(UndertowProcessBootstrap.class);

  Bootstrap bootstrap;

  private final List<Task> runningTasks = new ArrayList<>();

  private final Set<TaskStatusUpdateListener> statusUpdateListeners = new HashSet<>();

  public boolean addStatusUpdateListener(TaskStatusUpdateListener statusUpdateListener) {
    return statusUpdateListeners.add(statusUpdateListener);
  }

  public boolean removeStatusUpdateListener(TaskStatusUpdateListener statusUpdateListener) {
    return statusUpdateListeners.remove(statusUpdateListener);
  }

  public static void main(String[] args) throws Exception {
    new UndertowProcessBootstrap().start("localhost", 8080, null);
  }



  public void start(String host, int port, final Runnable onStart) throws InterruptedException {
    bootstrap = new Bootstrap(taskStatusUpdateListener());

    WebSocketBootstrap webSocketBootstrap = new WebSocketBootstrap(host, port, this, runningTasks);

    webSocketBootstrap.bootstrap(new Handler<Boolean>() {
      @Override
      public void handle(Boolean event) {
        if (event) {
          System.out.println("Server started on " + 8080);
          if (onStart != null) onStart.run();
        } else {
          System.out.println("Could not start");
        }
      }
    });
  }

  private TaskStatusUpdateListener taskStatusUpdateListener() {
    return (taskStatusUpdateEvent) -> {
      switch (taskStatusUpdateEvent.getNewStatus()) {
        case RUNNING:
          runningTasks.add(taskStatusUpdateEvent.getTask());
          break;

        case SUCCESSFULLY_COMPLETED:
        case FAILED:
        case INTERRUPTED:
          runningTasks.remove(taskStatusUpdateEvent.getTask());
      };
      notifyStatusUpdated(taskStatusUpdateEvent);
    };
  }

  void notifyStatusUpdated(TaskStatusUpdateEvent statusUpdateEvent) {
    for (TaskStatusUpdateListener statusUpdateListener : statusUpdateListeners) {
      log.debug("Notifying listener {} status update {}", statusUpdateListener, statusUpdateEvent.toJson());
      statusUpdateListener.accept(statusUpdateEvent);
    }
  }

  public Handler<TtyConnection> getBootstrap() {
    return bootstrap;
  }
}
