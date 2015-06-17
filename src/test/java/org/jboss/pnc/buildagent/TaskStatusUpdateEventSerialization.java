package org.jboss.pnc.buildagent;

import io.termd.core.Status;
import io.termd.core.http.Task;
import org.jboss.pnc.buildagent.spi.TaskStatusUpdateEvent;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
public class TaskStatusUpdateEventSerialization {

  private static final Logger log = LoggerFactory.getLogger(TaskStatusUpdateEventSerialization.class);

  @Test
  public void testTaskStatusUpdateEventSerialization() throws IOException {
    Task task = new Task(null, null, null, "");
    io.termd.core.http.TaskStatusUpdateEvent termTaskStatusUpdateEvent = new io.termd.core.http.TaskStatusUpdateEvent(task, Status.NEW, Status.RUNNING);

    TaskStatusUpdateEvent taskStatusUpdateEvent = new TaskStatusUpdateEvent(termTaskStatusUpdateEvent);
    String taskId = taskStatusUpdateEvent.getTaskId();

    String serialized = taskStatusUpdateEvent.toString();
    log.info("Serialized : {}", serialized);
    TaskStatusUpdateEvent deserializedTaskStatusUpdateEvent = TaskStatusUpdateEvent.fromJson(serialized);

    Assert.assertEquals(taskId, deserializedTaskStatusUpdateEvent.getTaskId());
  }
}
