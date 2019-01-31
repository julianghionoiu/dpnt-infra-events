package tdl.datapoint.infra_events;

import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;
import org.junit.rules.TemporaryFolder;
import org.yaml.snakeyaml.Yaml;
import tdl.datapoint.infra_events.support.LocalSQSQueue;
import tdl.datapoint.infra_events.support.S3Event;
import tdl.datapoint.infra_events.support.SNSEvent;
import tdl.participant.queue.connector.EventProcessingException;
import tdl.participant.queue.connector.QueueEventHandlers;
import tdl.participant.queue.connector.SqsEventQueue;
import tdl.participant.queue.events.RecorderStartedEvent;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Stack;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.allOf;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class RecorderStartedEventsAcceptanceTest {
    private static final Context NO_CONTEXT = null;
    private static final String UNSUPPORTED_S3_EVENT = "{\"Records\":[{Unsupported S3 event body}]";
    private static final String RECORDER_STARTED_EVENT = "{\"Records\":[{\"eventVersion\":\"2.1\",\"eventSource\":\"aws:s3\",\"awsRegion\":\"eu-west-2\",\"eventTime\":\"2019-01-11T21:07:25.954Z\",\"eventName\":\"ObjectCreated:Put\",\"userIdentity\":{\"principalId\":\"AWS:577770582757:tdl-live-mani_0110_01\"},\"requestParameters\":{\"sourceIPAddress\":\"91.110.160.204\"},\"responseElements\":{\"x-amz-request-id\":\"E996F81A6364D452\",\"x-amz-id-2\":\"LqI4jhOSDsCnIL/gW/pAFmAkhJbH2WJ+Kc6e16IwhgWorJwPavdfh60AUoNnksSSFMtxmtTV5j8=\"},\"s3\":{\"s3SchemaVersion\":\"1.0\",\"configurationId\":\"RecordingStarted\",\"bucket\":{\"name\":\"tdl-official-videos\",\"ownerIdentity\":{\"principalId\":\"A39KNTXUHOPHA4\"},\"arn\":\"arn:aws:s3:::tdl-official-videos\"},\"object\":{\"key\":\"HLO/mani_0110_01/last_sync_start.txt\",\"size\":24,\"eTag\":\"7065d91c3b36e89dfa23c6e7ce83af1a\",\"sequencer\":\"005C39058DE5F72FEF\"}}}]}";

    @Rule
    public EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private EventsAlertHandler eventsAlertHandler;
    private SqsEventQueue sqsEventQueue;
    private Stack<RecorderStartedEvent> recorderStartedEvents;
    private ObjectMapper mapper;

    @Before
    public void setUp() throws EventProcessingException, IOException {
        environmentVariables.set("AWS_ACCESS_KEY_ID","local_test_access_key");
        environmentVariables.set("AWS_SECRET_KEY","local_test_secret_key");
        setEnvFrom(environmentVariables, Paths.get("config", "env.local.yml"));

        sqsEventQueue = LocalSQSQueue.createInstance(
                getEnv(ApplicationEnv.SQS_ENDPOINT),
                getEnv(ApplicationEnv.SQS_REGION),
                getEnv(ApplicationEnv.SQS_QUEUE_URL));

        eventsAlertHandler = new EventsAlertHandler();

        QueueEventHandlers queueEventHandlers = new QueueEventHandlers();
        recorderStartedEvents = new Stack<>();
        queueEventHandlers.on(RecorderStartedEvent.class, recorderStartedEvents::add);
        sqsEventQueue.subscribeToMessages(queueEventHandlers);

        mapper = new ObjectMapper();
    }

    private static String getEnv(ApplicationEnv key) {
        String env = System.getenv(key.name());
        if (env == null || env.trim().isEmpty() || "null".equals(env)) {
            throw new RuntimeException("[Startup] Environment variable " + key + " not set");
        }
        return env;
    }

    private static void setEnvFrom(EnvironmentVariables environmentVariables, Path path) throws IOException {
        String yamlString = Files.lines(path).collect(Collectors.joining("\n"));

        Yaml yaml = new Yaml();
        Map<String, String> values = yaml.load(yamlString);

        values.forEach(environmentVariables::set);
    }

    @After
    public void tearDown() throws Exception {
        sqsEventQueue.unsubscribeFromMessages();
    }

    @Test
    public void start_recorder_and_an_event_should_flow_to_the_sqs_queue() throws Exception {
        // Given - The participant produces Video or Source files while solving a challenge
        String challengeId = generateId();
        String participantId = generateId();

        // When - Upload event happens
        S3Event s3PutEvent = new S3Event(RECORDER_STARTED_EVENT, challengeId, participantId);
        eventsAlertHandler.handleRequest(
                convertToMap(wrapAsSNSEvent(s3PutEvent)),
                NO_CONTEXT);

        // Then - event is wrapped and sent to the SQS queue
        waitForQueueToReceiveEvents();
        RecorderStartedEvent queueEvent = recorderStartedEvents.pop();
        String eventString = queueEvent.toString();  // eventually might be a idea to verify the event sent getEventAsJsonString();
        assertThat(eventString, allOf(containsString(participantId),
                containsString(challengeId)));
    }

    @Test
    public void an_unsupported_s3_event_should_not_flow_to_the_sqs_queue() {
        // Given - The participant does some other activity while solving a challenge
        String challengeId = generateId();
        String participantId = generateId();

        // When - Some unsupported event happens, let's say it's an unsupported S3 event in this case
        S3Event unsupportedS3Event = new S3Event(UNSUPPORTED_S3_EVENT, challengeId, participantId);
        try {
            eventsAlertHandler.handleRequest(
                    convertToMap(wrapAsSNSEvent(unsupportedS3Event)),
                    NO_CONTEXT);
            fail("No event should have been sent to the SQS queue");
        } catch (Exception ex) {
            // Then - check if the right exception related to unsupported event is been raised
            assertThat(ex.getMessage(),
                    containsString("An unidentified flying event has been detected, not letting it pass " +
                            "through the portal. Alerting the mother-ship by raising this exception."));
        }
    }

    private String wrapAsSNSEvent(S3Event s3Event) throws JsonProcessingException {
        SNSEvent snsEvent = new SNSEvent(mapper.writeValueAsString(s3Event.asJsonNode()));
        return mapper.writeValueAsString(snsEvent.asJsonNode());
    }

    //~~~~~~~~~~ Helpers ~~~~~~~~~~~~~`

    private static void waitForQueueToReceiveEvents() throws InterruptedException {
        Thread.sleep(500);
    }

    private static String generateId() {
        return UUID.randomUUID().toString().replaceAll("-","");
    }

    private static Map<String, Object> convertToMap(String json) throws IOException {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(json, new TypeReference<Map<String, Object>>() {});
    }
}
