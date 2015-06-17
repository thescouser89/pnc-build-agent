package org.jboss.pnc.buildagent.spi;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import io.termd.core.Status;

import java.io.IOException;

/**
 * @author <a href="mailto:matejonnet@gmail.com">Matej Lazar</a>
 */
class TaskStatusUpdateEventDeserializer extends JsonDeserializer<TaskStatusUpdateEvent> {
  @Override
  public TaskStatusUpdateEvent deserialize(JsonParser jp, DeserializationContext ctxt) throws IOException, JsonProcessingException {
    JsonNode node = jp.getCodec().readTree(jp);
    String taskId = node.get("taskId").asText();
    String oldStatus = node.get("oldStatus").asText();
    String newStatus = node.get("newStatus").asText();

    return new TaskStatusUpdateEvent(taskId, Status.valueOf(oldStatus), Status.valueOf(newStatus));
  }
}
