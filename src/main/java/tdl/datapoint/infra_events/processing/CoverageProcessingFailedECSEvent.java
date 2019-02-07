package tdl.datapoint.infra_events.processing;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.Map;

public class CoverageProcessingFailedECSEvent extends FailedECSEvent {

    private final String roundId;

    private CoverageProcessingFailedECSEvent(String eventJson,
                                             String participantId,
                                             String roundId,
                                             String errorMessage) {
        super(eventJson, participantId, errorMessage);
        this.roundId = roundId;
    }

    @SuppressWarnings("unchecked")
    public static CoverageProcessingFailedECSEvent from(Map<String, Object> request,
                                                        ObjectMapper jsonObjectMapper) throws IOException {
        JsonNode ecsObject = getRecordsNode(request, jsonObjectMapper);

        String eventJson = ecsObject.get("ecsevent").get("eventJson").asText();
        String participantId = ecsObject.get("ecsevent").get("participantId").asText();
        String errorMessage = ecsObject.get("ecsevent").get("errorMessage").asText();
        String roundId = ecsObject.get("ecsevent").get("roundId").asText();

        return new CoverageProcessingFailedECSEvent(
                eventJson,
                participantId,
                roundId,
                errorMessage
        );
    }

    public String getRoundId() {
        return roundId;
    }

    @Override
    public String toString() {
        return "CoverageProcessingFailedECSEvent{" +
                "eventJson='" + eventJson + '\'' +
                ", participantId='" + participantId + '\'' +
                ", roundId='" + roundId + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
