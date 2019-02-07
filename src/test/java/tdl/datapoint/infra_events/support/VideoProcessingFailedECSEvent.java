package tdl.datapoint.infra_events.support;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class VideoProcessingFailedECSEvent {
    private final String eventJson;
    private final String challengeId;
    private final String participantId;
    private final String errorMessage;

    public VideoProcessingFailedECSEvent(String eventJson,
                                         String participantId,
                                         String challengeId,
                                         String errorMessage) {
        this.eventJson = eventJson;
        this.participantId = participantId;
        this.challengeId = challengeId;
        this.errorMessage = errorMessage;
    }

    public ObjectNode asJsonNode() {
        final JsonNodeFactory factory = JsonNodeFactory.instance;

        ObjectNode rootNode = factory.objectNode();
        ObjectNode ecsEventNode = rootNode.putArray("Records")
                                          .addObject()
                                          .putObject("ecsevent");
        ecsEventNode.put("eventJson", eventJson)
                    .put("participantId", participantId)
                    .put("challengeId", challengeId)
                    .put("errorMessage", errorMessage);

        return rootNode;
    }
}
