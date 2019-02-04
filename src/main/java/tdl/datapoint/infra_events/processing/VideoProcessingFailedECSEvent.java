package tdl.datapoint.infra_events.processing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

public class VideoProcessingFailedECSEvent extends FailedECSEvent {

    private final String challengeId;

    private VideoProcessingFailedECSEvent(String eventJson,
                                          String challengeId,
                                          String participantId,
                                          String errorMessage) {
        super(eventJson, participantId, errorMessage);
        this.challengeId = challengeId;
    }

    @SuppressWarnings("unchecked")
    public static VideoProcessingFailedECSEvent from(Map<String, Object> request,
                                                     ObjectMapper jsonObjectMapper) throws IOException {
        FailedECSEvent failedECSEvent = FailedECSEvent.from(request, jsonObjectMapper);
        JsonNode ecsObject = getRecordsNode(request, jsonObjectMapper);
        String challengeId = ecsObject.get("ecsevent").get("challengeId").asText();
        return new VideoProcessingFailedECSEvent(
                failedECSEvent.getEventJson(),
                challengeId,
                failedECSEvent.getParticipantId(),
                failedECSEvent.getErrorMessage()
        );

    }

    public String getChallengeId() {
        return challengeId;
    }

    @Override
    public String toString() {
        return "VideoProcessingFailedECSEvent{" +
                "eventJson='" + eventJson + '\'' +
                ", challengeId='" + challengeId + '\'' +
                ", participantId='" + participantId + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
