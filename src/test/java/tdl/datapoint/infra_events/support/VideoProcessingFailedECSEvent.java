package tdl.datapoint.infra_events.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class VideoProcessingFailedECSEvent extends FailedECSEvent {
    private final String challengeId;

    public VideoProcessingFailedECSEvent(String eventJson,
                                         String challengeId,
                                         String participantId,
                                         String errorMessage) {
        super(eventJson, participantId, errorMessage);
        this.challengeId = challengeId;
    }

    public ObjectNode asJsonNode() {
        ObjectNode rootNode = super.asJsonNode();
        JsonNode ecsEvent = rootNode.get("Records").get(0).get("ecsevent");
        ((ObjectNode)ecsEvent).put("challengeId", challengeId);
        return rootNode;
    }
}
