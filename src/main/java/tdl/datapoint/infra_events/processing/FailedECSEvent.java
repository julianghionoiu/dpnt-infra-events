package tdl.datapoint.infra_events.processing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class FailedECSEvent {
    protected final String eventJson;
    protected final String participantId;
    protected final String errorMessage;

    FailedECSEvent(String eventJson, String participantId, String errorMessage) {
        this.eventJson = eventJson;
        this.participantId = participantId;
        this.errorMessage = errorMessage;
    }

    @SuppressWarnings("unchecked")
    static FailedECSEvent from(Map<String, Object> request,
                                                        ObjectMapper jsonObjectMapper) throws IOException {
        if (request == null) {
            throw new IllegalArgumentException("No input provided");
        }

        JsonNode ecsObject = getRecordsNode(request, jsonObjectMapper);

        String eventJson = ecsObject.get("ecsevent").get("eventJson").asText();
        String participantId = ecsObject.get("ecsevent").get("participantId").asText();
        String errorMessage = ecsObject.get("ecsevent").get("errorMessage").asText();
        return new FailedECSEvent(eventJson, participantId, errorMessage);
    }

    static JsonNode getRecordsNode(Map<String, Object> request, ObjectMapper jsonObjectMapper) throws IOException {
        Map<String, Object> record = ((List<Map<String, Object>>) mapGet(request, "Records")).get(0);
        Map<String, Object> sns = (Map<String, Object>) mapGet(record, "Sns");
        String jsonECSPayload = (String) mapGet(sns, "Message");

        JsonNode ecsEventTree = jsonObjectMapper.readTree(jsonECSPayload);
        return ecsEventTree.get("Records").get(0);
    }

    private static Object mapGet(Map<String, Object> map, String key) {
        if (map == null) {
            throw new IllegalArgumentException("No input provided. Map is \"null\".");
        }

        Object o = map.get(key);
        if (o == null) {
            throw new IllegalArgumentException(String.format("Key \"%s\" not found in map.", key));
        }
        return o;
    }

    public String getEventJson() {
        return eventJson;
    }

    public String getParticipantId() {
        return participantId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return "FailedECSEvent{" +
                "eventJson='" + eventJson + '\'' +
                ", participantId='" + participantId + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
