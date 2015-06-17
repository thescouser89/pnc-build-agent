package org.jboss.pnc.buildagent.spi;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import io.termd.core.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
@JsonDeserialize(using = TaskStatusUpdateEventDeserializer.class)
public class TaskStatusUpdateEvent implements Serializable {

  private static final Logger log = LoggerFactory.getLogger(TaskStatusUpdateEvent.class);

  private final String taskId;
  private final Status oldStatus;
  private final Status newStatus;

  public TaskStatusUpdateEvent(io.termd.core.http.TaskStatusUpdateEvent taskStatusUpdateEvent) {
    taskId = taskStatusUpdateEvent.getTask().getId() + "";
    oldStatus = taskStatusUpdateEvent.getOldStatus();
    newStatus = taskStatusUpdateEvent.getNewStatus();
  }

  public TaskStatusUpdateEvent(String taskId, Status oldStatus, Status newStatus) {
    this.taskId = taskId;
    this.oldStatus = oldStatus;
    this.newStatus = newStatus;
  }

  public String getTaskId() {
    return taskId;
  }

  public Status getOldStatus() {
    return oldStatus;
  }

  public Status getNewStatus() {
    return newStatus;
  }

  public String toString() {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      log.error("Cannot serialize object.", e);
    }
    return null; //TODO ?
  }

  public static TaskStatusUpdateEvent fromJson(String serialized) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    try {
      return mapper.readValue(serialized, TaskStatusUpdateEvent.class);
    } catch (JsonParseException | JsonMappingException e) {
      log.error("Cannot deserialize object from json", e);
      throw e;
    }
  }

}
