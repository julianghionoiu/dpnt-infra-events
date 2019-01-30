package tdl.datapoint.infra_events;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import tdl.datapoint.infra_events.processing.S3BucketEvent;
import tdl.participant.queue.connector.SqsEventQueue;
import tdl.participant.queue.events.RecorderStartedEvent;

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
            if (containsEvent("aws:s3", inEventMap)) {
                handleS3Event(S3BucketEvent.from(inEventMap, jsonObjectMapper));
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

    private boolean containsEvent(String eventType, Map<String, Object> eventMap) {
        return getMessageStringFrom(eventMap).contains(eventType);
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
}
