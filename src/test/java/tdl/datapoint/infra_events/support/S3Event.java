package tdl.datapoint.infra_events.support;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class S3Event {
    private final String eventJson;
    private final String challengeId;
    private final String participantId;

    public S3Event(String eventJson, String challengeId, String participantId) {
        this.eventJson = eventJson;
        this.challengeId = challengeId;
        this.participantId = participantId;
    }

    public ObjectNode asJsonNode() {
        final JsonNodeFactory factory = JsonNodeFactory.instance;

        ObjectNode rootNode = factory.objectNode();
        ObjectNode s3 = rootNode.putArray("Records").addObject().putObject("s3");
        s3.putObject("s3event")
                .put("eventJson", eventJson)
                .put("challengeId", challengeId)
                .put("participantId", participantId);
        return rootNode;
    }
}
