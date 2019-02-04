package tdl.datapoint.infra_events.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CoverageProcessingFailedECSEvent extends FailedECSEvent {
    private final String roundId;

    public CoverageProcessingFailedECSEvent(String eventJson,
                                            String roundId,
                                            String participantId,
                                            String errorMessage) {
        super(eventJson, participantId, errorMessage);
        this.roundId = roundId;
    }

    public ObjectNode asJsonNode() {
        ObjectNode rootNode = super.asJsonNode();
        JsonNode ecsEvent = rootNode.get("Records").get(0).get("ecsevent");
        ((ObjectNode)ecsEvent).put("roundId", roundId);
        return rootNode;
    }
}
