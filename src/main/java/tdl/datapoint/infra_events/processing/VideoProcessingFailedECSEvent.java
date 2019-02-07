package tdl.datapoint.infra_events.processing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

public class VideoProcessingFailedECSEvent extends FailedECSEvent {

    private final String challengeId;

    private VideoProcessingFailedECSEvent(String eventJson,
                                          String participantId,
                                          String challengeId,
                                          String errorMessage) {
        super(eventJson, participantId, errorMessage);
        this.challengeId = challengeId;
    }

    @SuppressWarnings("unchecked")
    public static VideoProcessingFailedECSEvent from(Map<String, Object> request,
                                                     ObjectMapper jsonObjectMapper) throws IOException {
        JsonNode ecsObject = getRecordsNode(request, jsonObjectMapper);

        String eventJson = ecsObject.get("ecsevent").get("eventJson").asText();
        String participantId = ecsObject.get("ecsevent").get("participantId").asText();
        String errorMessage = ecsObject.get("ecsevent").get("errorMessage").asText();
        String challengeId = ecsObject.get("ecsevent").get("challengeId").asText();

        return new VideoProcessingFailedECSEvent(
                eventJson,
                participantId,
                challengeId,
                errorMessage
        );
    }

    public String getChallengeId() {
        return challengeId;
    }

    @Override
    public String toString() {
        return "VideoProcessingFailedECSEvent{" +
                "eventJson='" + eventJson + '\'' +
                ", participantId='" + participantId + '\'' +
                ", challengeId='" + challengeId + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
