package tdl.datapoint.infra_events;

import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestHandler;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.sqs.AmazonSQS;
import com.amazonaws.services.sqs.AmazonSQSClientBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import tdl.datapoint.infra_events.processing.S3BucketEvent;
import tdl.participant.queue.connector.SqsEventQueue;
import tdl.participant.queue.events.SourceCodeUpdatedEvent;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static tdl.datapoint.infra_events.ApplicationEnv.S3_ENDPOINT;
import static tdl.datapoint.infra_events.ApplicationEnv.S3_REGION;
import static tdl.datapoint.infra_events.ApplicationEnv.SQS_ENDPOINT;
import static tdl.datapoint.infra_events.ApplicationEnv.SQS_QUEUE_URL;
import static tdl.datapoint.infra_events.ApplicationEnv.SQS_REGION;

public class SourceCodeUploadHandler implements RequestHandler<Map<String, Object>, String> {
    private static final Logger LOG = Logger.getLogger(SourceCodeUploadHandler.class.getName());
    private AmazonS3 s3Client;
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
    public SourceCodeUploadHandler() {
        s3Client = createS3Client(
                getEnv(S3_ENDPOINT),
                getEnv(S3_REGION));

        AmazonSQS client = createSQSClient(
                getEnv(SQS_ENDPOINT),
                getEnv(SQS_REGION)
        );

        String queueUrl = getEnv(SQS_QUEUE_URL);
        participantEventQueue = new SqsEventQueue(client, queueUrl);

        jsonObjectMapper = new ObjectMapper();
    }

    private static AmazonS3 createS3Client(String endpoint, String region) {
        AmazonS3ClientBuilder builder = AmazonS3ClientBuilder.standard();
        builder = builder.withPathStyleAccessEnabled(true)
                .withEndpointConfiguration(new AwsClientBuilder.EndpointConfiguration(endpoint, region))
                .withCredentials(new DefaultAWSCredentialsProviderChain());
        return builder.build();
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
    public String handleRequest(Map<String, Object> s3EventMap, Context context) {
        try {
            handleS3Event(S3BucketEvent.from(s3EventMap, jsonObjectMapper));
            return "OK";
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, ex.getMessage(), ex);
            throw new RuntimeException(ex);
        }
    }

    private void handleS3Event(S3BucketEvent event) throws Exception {
        LOG.info("Process S3 event with: "+event);
        String participantId = event.getParticipantId();
        String challengeId = event.getChallengeId();

        participantEventQueue.send(new SourceCodeUpdatedEvent(System.currentTimeMillis(),
                        participantId, challengeId, "http://some url in http format"));
    }

}
