package tdl.datapoint.infra_events.support;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class FailedECSEvent {
    private final String eventJson;
    private final String participantId;
    private final String errorMessage;

    FailedECSEvent(String eventJson, String participantId, String errorMessage) {
        this.eventJson = eventJson;
        this.participantId = participantId;
        this.errorMessage = errorMessage;
    }

    public ObjectNode asJsonNode() {
        final JsonNodeFactory factory = JsonNodeFactory.instance;

        ObjectNode rootNode = factory.objectNode();
        ObjectNode s3 = rootNode.putArray("Records").addObject().putObject("ecsevent");
        s3.put("eventJson", eventJson)
          .put("participantId", participantId)
          .put("errorMessage", errorMessage);
        return rootNode;
    }
}
