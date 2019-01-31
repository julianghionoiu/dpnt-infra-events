package tdl.datapoint.infra_events.processing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class ECSEvent {

    private final String eventJson;
    private final String challengeId;
    private final String participantId;

    private ECSEvent(String eventJson, String challengeId, String participantId) {
        this.eventJson = eventJson;
        this.challengeId = challengeId;
        this.participantId = participantId;
    }

    @SuppressWarnings("unchecked")
    public static ECSEvent from(Map<String, Object> request,
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
        String challengeId = ecsObject.get("ecsevent").get("challengeId").asText();
        String participantId = ecsObject.get("ecsevent").get("participantId").asText();
        return new ECSEvent(eventJson, challengeId, participantId);
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

    public String getChallengeId() {
        return challengeId;
    }

    public String getParticipantId() {
        return participantId;
    }

    @Override
    public String toString() {
        return "ECSEvent{" +
                "eventJson='" + eventJson + '\'' +
                ", challengeId='" + challengeId + '\'' +
                ", participantId='" + participantId + '\'' +
                '}';
    }
}
