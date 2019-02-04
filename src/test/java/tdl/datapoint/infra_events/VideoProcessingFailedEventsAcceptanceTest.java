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
import tdl.datapoint.infra_events.support.VideoProcessingFailedECSEvent;
import tdl.datapoint.infra_events.support.LocalSQSQueue;
import tdl.datapoint.infra_events.support.SNSEvent;
import tdl.participant.queue.connector.EventProcessingException;
import tdl.participant.queue.connector.QueueEventHandlers;
import tdl.participant.queue.connector.SqsEventQueue;
import tdl.participant.queue.events.VideoProcessingFailedEvent;

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

public class VideoProcessingFailedEventsAcceptanceTest {
    private static final Context NO_CONTEXT = null;
    private static final String UNSUPPORTED_ECS_EVENT = "{{Unsupported ECS event body}";
    private static final String ECS_VIDEO_PROCESSING_FAILED_EVENT = "{\"version\":\"0\",\"id\":\"305f07f3-c812-e2a5-e7fa-626f6ac46ce1\",\"detail-type\":\"ECS Task State Change\",\"source\":\"aws.ecs\",\"account\":\"577770582757\",\"time\":\"2018-09-02T16:14:16Z\",\"region\":\"eu-west-1\",\"resources\":[\"arn:aws:ecs:eu-west-1:577770582757:task/817dcb71-de09-4b7c-af12-b73eb7699bff\"],\"detail\":{\"clusterArn\":\"arn:aws:ecs:eu-west-1:577770582757:cluster/dpnt-coverage-live\",\"containerInstanceArn\":\"arn:aws:ecs:eu-west-1:577770582757:container-instance/c8291511-dfc2-4b19-b7cb-496b59a6becf\",\"containers\":[{\"containerArn\":\"arn:aws:ecs:eu-west-1:577770582757:container/72c5f96c-6e65-4528-b42d-3cf3e52f74d8\",\"exitCode\":255,\"lastStatus\":\"STOPPED\",\"name\":\"default-container\",\"taskArn\":\"arn:aws:ecs:eu-west-1:577770582757:task/817dcb71-de09-4b7c-af12-b73eb7699bff\",\"networkInterfaces\":[]}],\"createdAt\":\"2018-09-02T16:11:26.191Z\",\"launchType\":\"FARGATE\",\"cpu\":\"1024\",\"memory\":\"2048\",\"desiredStatus\":\"STOPPED\",\"group\":\"family:dpnt-coverage-live-nodejs\",\"lastStatus\":\"STOPPED\",\"overrides\":{\"containerOverrides\":[{\"environment\":[{\"name\":\"PARTICIPANT_ID\",\"value\":\"hyks01\"},{\"name\":\"REPO\",\"value\":\"s3://tdl-official-videos/CHK/hyks01/sourcecode_20180902T113408.srcs\"},{\"name\":\"CHALLENGE_ID\",\"value\":\"CHK\"},{\"name\":\"ROUND_ID\",\"value\":\"CHK_R2\"},{\"name\":\"TAG\",\"value\":\"CHK_R2/done\"}],\"name\":\"default-container\"}]},\"connectivity\":\"CONNECTED\",\"pullStartedAt\":\"2018-09-02T16:11:37.959Z\",\"startedAt\":\"2018-09-02T16:12:57.959Z\",\"stoppingAt\":\"2018-09-02T16:14:03.598Z\",\"stoppedAt\":\"2018-09-02T16:14:16.312Z\",\"pullStoppedAt\":\"2018-09-02T16:12:57.959Z\",\"executionStoppedAt\":\"2018-09-02T16:14:03Z\",\"stoppedReason\":\"Essential container in task exited\",\"updatedAt\":\"2018-09-02T16:14:16.312Z\",\"taskArn\":\"arn:aws:ecs:eu-west-1:577770582757:task/817dcb71-de09-4b7c-af12-b73eb7699bff\",\"taskDefinitionArn\":\"arn:aws:ecs:eu-west-1:577770582757:task-definition/dpnt-coverage-live-nodejs:1\",\"version\":5}}";

    @Rule
    public EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private EventsAlertHandler eventsAlertHandler;
    private SqsEventQueue sqsEventQueue;
    private Stack<VideoProcessingFailedEvent> videoProcessingFailedEvents;
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

        videoProcessingFailedEvents = new Stack<>();
        queueEventHandlers.on(VideoProcessingFailedEvent.class, videoProcessingFailedEvents::add);

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
    public void when_video_processing_fails_in_a_container_an_event_should_flow_to_the_sqs_queue() throws Exception {
        // Given - The participant has uploaded a video while solving a challenge
        String challengeId = generateId();
        String participantId = generateId();
        String errorMessage = "Essential container in task exited" ;

        // When - Video processing fails in the container on the ECS
        VideoProcessingFailedECSEvent ecsEvent = new VideoProcessingFailedECSEvent(
                ECS_VIDEO_PROCESSING_FAILED_EVENT,
                challengeId,
                participantId,
                errorMessage
        );
        eventsAlertHandler.handleRequest(
                convertToMap(wrapAsSNSEvent(ecsEvent)),
                NO_CONTEXT);

        // Then - a failure event is wrapped and sent to the SQS queue
        waitForQueueToReceiveEvents();
        VideoProcessingFailedEvent queueEvent = videoProcessingFailedEvents.pop();
        String eventString = queueEvent.toString();  // eventually might be a idea to verify the event sent getEventAsJsonString();
        assertThat(eventString,
                allOf(containsString(participantId),
                        containsString(challengeId),
                        containsString(errorMessage))
        );
    }

    @Test
    public void an_unsupported_ecs_event_should_not_flow_to_the_sqs_queue() {
        // Given - The participant does some other activity while solving a challenge
        String challengeId = generateId();
        String participantId = generateId();
        String errorMessage = "";

        // When - Some unsupported event happens, let's say it's an unsupported ECS event in this case
        VideoProcessingFailedECSEvent unsupportedECSEvent = new VideoProcessingFailedECSEvent(
                UNSUPPORTED_ECS_EVENT,
                challengeId,
                participantId,
                errorMessage
        );
        try {
            eventsAlertHandler.handleRequest(
                    convertToMap(wrapAsSNSEvent(unsupportedECSEvent)),
                    NO_CONTEXT);
            fail("No event should have been sent to the SQS queue");
        } catch (Exception ex) {
            // Then - check if the right exception related to unsupported event is been raised
            assertThat(ex.getMessage(),
                    containsString("An unidentified flying event has been detected, not letting it pass " +
                            "through the portal. Alerting the mother-ship by raising this exception."));
        }
    }

    private String wrapAsSNSEvent(VideoProcessingFailedECSEvent ecsEvent) throws JsonProcessingException {
        SNSEvent snsEvent = new SNSEvent(mapper.writeValueAsString(ecsEvent.asJsonNode()));
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
