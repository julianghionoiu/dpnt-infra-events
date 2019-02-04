package tdl.datapoint.infra_events;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import tdl.datapoint.infra_events.processing.CoverageProcessingFailedECSEvent;
import tdl.datapoint.infra_events.processing.S3BucketEvent;
import tdl.datapoint.infra_events.processing.VideoProcessingFailedECSEvent;
import tdl.participant.queue.connector.SqsEventQueue;
import tdl.participant.queue.events.CoverageProcessingFailedEvent;
import tdl.participant.queue.events.RecorderStartedEvent;
import tdl.participant.queue.events.VideoProcessingFailedEvent;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tdl.datapoint.infra_events.ApplicationEnv.SQS_ENDPOINT;
import static tdl.datapoint.infra_events.ApplicationEnv.SQS_QUEUE_URL;
import static tdl.datapoint.infra_events.ApplicationEnv.SQS_REGION;

public class EventsAlertHandler implements RequestHandler<Map<String, Object>, String> {
    private static final Logger LOG = Logger.getLogger(EventsAlertHandler.class.getName());

    private SqsEventQueue participantEventQueue;
    private ObjectMapper jsonObjectMapper;


    private static String getEnv(ApplicationEnv key) {
        String env = System.getenv(key.name());
        if (env == null || env.trim().isEmpty() || "null".equals(env)) {
            throw new RuntimeException("[Startup] Environment variable " + key + " not set");
        }
        return env;
    }

    @SuppressWarnings("WeakerAccess")
    public EventsAlertHandler() {
        AmazonSQS client = createSQSClient(
                getEnv(SQS_ENDPOINT),
                getEnv(SQS_REGION)
        );

        String queueUrl = getEnv(SQS_QUEUE_URL);
        participantEventQueue = new SqsEventQueue(client, queueUrl);

        jsonObjectMapper = new ObjectMapper();
    }

    private static AmazonSQS createSQSClient(String serviceEndpoint, String signingRegion) {
        AwsClientBuilder.EndpointConfiguration endpointConfiguration =
                new AwsClientBuilder.EndpointConfiguration(serviceEndpoint, signingRegion);
        return AmazonSQSClientBuilder.standard()
                .withEndpointConfiguration(endpointConfiguration)
                .withCredentials(new DefaultAWSCredentialsProviderChain())
                .build();
    }

    @Override
    public String handleRequest(Map<String, Object> inEventMap, Context context) {
        try {
            if (containsEvent(Arrays.asList("aws:s3"), inEventMap)) {
                handleS3Event(S3BucketEvent.from(inEventMap, jsonObjectMapper));
                return "OK";
            }

            if (containsEvent(
                  Arrays.asList("aws.ecs", "pullStartedAt", "pullStoppedAt", "containerInstanceArn"),
                  inEventMap)
            ) {
                handleECSVideoEvent(VideoProcessingFailedECSEvent.from(inEventMap, jsonObjectMapper));
                return "OK";
            }

            if (containsEvent(Arrays.asList("aws.ecs", "attachments"), inEventMap) &&
                ! containsEvent(Arrays.asList("pullStartedAt", "pullStoppedAt", "containerInstanceArn"), inEventMap)
            ) {
                handleECSCoverageEvent(CoverageProcessingFailedECSEvent.from(inEventMap, jsonObjectMapper));
                return "OK";
            }

            throw new RuntimeException(
                    "An unidentified flying event has been detected, not letting it pass through " +
                            "the portal. Alerting the mother-ship by raising this exception.");
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    private boolean containsEvent(List<String> eventAttributes, Map<String, Object> eventMap) {
        for (String eachAttribute: eventAttributes) {
            if (!getMessageStringFrom(eventMap).contains(eachAttribute)) {
                return false;
            }
        }
        return true;
    }

    private String getMessageStringFrom(Map<String, Object> eventMap) {
        List<LinkedHashMap> records = (List<LinkedHashMap>) eventMap.get("Records");
        if (records != null) {
            LinkedHashMap firstRecord = records.get(0);
            if (firstRecord != null) {
                LinkedHashMap snsObject = (LinkedHashMap) firstRecord.get("Sns");

            return (String) snsObject.get("Message");
            }
        }

        return "";
    }

    private void handleS3Event(S3BucketEvent event) throws Exception {
        LOG.info("Process S3 event with: " + event);
        String challengeId = event.getChallengeId();
        String participantId = event.getParticipantId();

        participantEventQueue.send(new RecorderStartedEvent(System.currentTimeMillis(),
                        participantId, challengeId));
    }

    private void handleECSVideoEvent(VideoProcessingFailedECSEvent event) throws Exception {
        LOG.info("Process ECS Video Processing Failure event with: " + event);
        String challengeId = event.getChallengeId();
        String participantId = event.getParticipantId();
        String errorMessage = event.getErrorMessage();

        participantEventQueue.send(new VideoProcessingFailedEvent(System.currentTimeMillis(),
                participantId, challengeId, errorMessage));
    }

    private void handleECSCoverageEvent(CoverageProcessingFailedECSEvent event) throws Exception {
        LOG.info("Process ECS Coverage Processing Failure event with: " + event);
        String roundId = event.getRoundId();
        String participantId = event.getParticipantId();
        String errorMessage = event.getErrorMessage();

        participantEventQueue.send(new CoverageProcessingFailedEvent(System.currentTimeMillis(),
                participantId, roundId, errorMessage));
    }
}
