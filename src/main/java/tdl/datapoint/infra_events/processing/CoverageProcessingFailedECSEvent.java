package tdl.datapoint.infra_events.processing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

public class CoverageProcessingFailedECSEvent extends FailedECSEvent {

    private final String roundId;

    private CoverageProcessingFailedECSEvent(String eventJson,
                                             String roundId,
                                             String participantId,
                                             String errorMessage) {
        super(eventJson, participantId, errorMessage);
        this.roundId = roundId;
    }

    @SuppressWarnings("unchecked")
    public static CoverageProcessingFailedECSEvent from(Map<String, Object> request,
                                                        ObjectMapper jsonObjectMapper) throws IOException {

        FailedECSEvent failedECSEvent = FailedECSEvent.from(request, jsonObjectMapper);
        JsonNode ecsObject = getRecordsNode(request, jsonObjectMapper);
        String roundId = ecsObject.get("ecsevent").get("roundId").asText();
        return new CoverageProcessingFailedECSEvent(
                failedECSEvent.getEventJson(),
                roundId,
                failedECSEvent.getParticipantId(),
                failedECSEvent.getErrorMessage()
        );
    }

    public String getRoundId() {
        return roundId;
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
