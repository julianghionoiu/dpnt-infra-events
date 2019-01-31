package tdl.datapoint.infra_events.support;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class ECSEvent {
    private final String eventJson;
    private final String challengeId;
    private final String participantId;

    public ECSEvent(String eventJson, String challengeId, String participantId) {
        this.eventJson = eventJson;
        this.challengeId = challengeId;
        this.participantId = participantId;
    }

    public ObjectNode asJsonNode() {
        final JsonNodeFactory factory = JsonNodeFactory.instance;

        ObjectNode rootNode = factory.objectNode();
        ObjectNode s3 = rootNode.putArray("Records").addObject().putObject("ecsevent");
        s3.putObject("ecsevent")
                .put("eventJson", eventJson)
                .put("challengeId", challengeId)
                .put("participantId", participantId);
        return rootNode;
    }
}
