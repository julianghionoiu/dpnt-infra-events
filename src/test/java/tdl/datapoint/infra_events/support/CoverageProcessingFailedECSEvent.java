package tdl.datapoint.infra_events.support;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CoverageProcessingFailedECSEvent {
    private final String eventJson;
    private final String roundId;
    private final String participantId;
    private final String errorMessage;

    public CoverageProcessingFailedECSEvent(String eventJson,
                                            String roundId,
                                            String participantId,
                                            String errorMessage) {
        this.eventJson = eventJson;
        this.roundId = roundId;
        this.participantId = participantId;
        this.errorMessage = errorMessage;
    }

    public ObjectNode asJsonNode() {
        final JsonNodeFactory factory = JsonNodeFactory.instance;

        ObjectNode rootNode = factory.objectNode();
        ObjectNode s3 = rootNode.putArray("Records").addObject().putObject("ecsevent");
        s3.putObject("ecsevent")
                .put("eventJson", eventJson)
                .put("roundId", roundId)
                .put("participantId", participantId)
                .put("errorMessage", errorMessage);
        return rootNode;
    }
}
