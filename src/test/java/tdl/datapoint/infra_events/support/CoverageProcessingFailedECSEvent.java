package tdl.datapoint.infra_events.support;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class CoverageProcessingFailedECSEvent {
    private final String eventJson;
    private final String participantId;
    private final String roundId;
    private final String errorMessage;

    public CoverageProcessingFailedECSEvent(String eventJson,
                                            String participantId,
                                            String roundId,
                                            String errorMessage) {
        this.eventJson = eventJson;
        this.participantId = participantId;
        this.roundId = roundId;
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
                    .put("roundId", roundId)
                    .put("errorMessage", errorMessage);

        return rootNode;
    }
}
