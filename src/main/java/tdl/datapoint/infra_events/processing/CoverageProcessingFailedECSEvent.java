package tdl.datapoint.infra_events.processing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class CoverageProcessingFailedECSEvent {

    private final String eventJson;
    private final String roundId;
    private final String participantId;
    private final String errorMessage;

    private CoverageProcessingFailedECSEvent(String eventJson,
                                             String roundId,
                                             String participantId,
                                             String errorMessage) {
        this.eventJson = eventJson;
        this.roundId = roundId;
        this.participantId = participantId;
        this.errorMessage = errorMessage;
    }

    @SuppressWarnings("unchecked")
    public static CoverageProcessingFailedECSEvent from(Map<String, Object> request,
                                ObjectMapper jsonObjectMapper) throws IOException {
        if (request == null) {
            throw new IllegalArgumentException("No input provided");
        }

        Map<String, Object> record = ((List<Map<String, Object>>) mapGet(request, "Records")).get(0);
        Map<String, Object> sns = (Map<String, Object>) mapGet(record, "Sns");
        String jsonECSPayload = (String) mapGet(sns, "Message");


        JsonNode ecsEventTree = jsonObjectMapper.readTree(jsonECSPayload);
        JsonNode ecsObject = ecsEventTree.get("Records").get(0).get("ecsevent");

        String eventJson = ecsObject.get("ecsevent").get("eventJson").asText();
        String roundId = ecsObject.get("ecsevent").get("roundId").asText();
        String participantId = ecsObject.get("ecsevent").get("participantId").asText();
        String errorMessage = ecsObject.get("ecsevent").get("errorMessage").asText();
        return new CoverageProcessingFailedECSEvent(eventJson, roundId, participantId, errorMessage);
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

    public String getRoundId() {
        return roundId;
    }

    public String getParticipantId() {
        return participantId;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return "CoverageProcessingFailedECSEvent{" +
                "eventJson='" + eventJson + '\'' +
                ", roundId='" + roundId + '\'' +
                ", participantId='" + participantId + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
