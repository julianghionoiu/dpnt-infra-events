package tdl.datapoint.infra_events.processing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class S3BucketEvent {

    private final String eventJson;
    private final String challengeId;
    private final String participantId;

    private S3BucketEvent(String eventJson, String challengeId, String participantId) {
        this.eventJson = eventJson;
        this.challengeId = challengeId;
        this.participantId = participantId;
    }

    @SuppressWarnings("unchecked")
    public static S3BucketEvent from(Map<String, Object> request,
                                     ObjectMapper jsonObjectMapper) throws IOException {
        if (request == null) {
            throw new IllegalArgumentException("No input provided");
        }

        Map<String, Object> record = ((List<Map<String, Object>>) mapGet(request, "Records")).get(0);
        Map<String, Object> sns = (Map<String, Object>) mapGet(record, "Sns");
        String jsonS3Payload = (String) mapGet(sns, "Message");


        JsonNode s3EventTree = jsonObjectMapper.readTree(jsonS3Payload);
        JsonNode s3Object = s3EventTree.get("Records").get(0).get("s3");

        String eventJson = s3Object.get("s3event").get("eventJson").asText();
        String challengeId = s3Object.get("s3event").get("challengeId").asText();
        String participantId = s3Object.get("s3event").get("participantId").asText();
        return new S3BucketEvent(eventJson, challengeId, participantId);
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
        return "S3BucketEvent{" +
                "eventJson='" + eventJson + '\'' +
                ", challengeId='" + challengeId + '\'' +
                ", participantId='" + participantId + '\'' +
                '}';
    }
}
