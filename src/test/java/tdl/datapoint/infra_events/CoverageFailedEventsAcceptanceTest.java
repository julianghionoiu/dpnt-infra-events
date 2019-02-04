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
import tdl.datapoint.infra_events.support.CoverageProcessingFailedECSEvent;
import tdl.datapoint.infra_events.support.LocalSQSQueue;
import tdl.datapoint.infra_events.support.SNSEvent;
import tdl.participant.queue.connector.EventProcessingException;
import tdl.participant.queue.connector.QueueEventHandlers;
import tdl.participant.queue.connector.SqsEventQueue;
import tdl.participant.queue.events.CoverageProcessingFailedEvent;

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

public class CoverageFailedEventsAcceptanceTest {
    private static final Context NO_CONTEXT = null;
    private static final String UNSUPPORTED_ECS_EVENT = "{{Unsupported ECS event body}";
    private static final String ECS_COVERAGE_FAILED_EVENT = "{\"version\":\"0\",\"id\":\"30ca1d6a-80b8-ecf9-4cdc-3531554d407a\",\"detail-type\":\"ECS Task State Change\",\"source\":\"aws.ecs\",\"account\":\"577770582757\",\"time\":\"2018-11-25T13:48:32Z\",\"region\":\"eu-west-1\",\"resources\":[\"arn:aws:ecs:eu-west-1:577770582757:task/48b872ca-a6d6-408c-ab78-c6a9161dc453\"],\"detail\":{\"clusterArn\":\"arn:aws:ecs:eu-west-1:577770582757:cluster/dpnt-coverage-live\",\"containers\":[{\"containerArn\":\"arn:aws:ecs:eu-west-1:577770582757:container/2d73eb67-2216-4202-b008-27ebba56c1be\",\"lastStatus\":\"STOPPED\",\"name\":\"default-container\",\"taskArn\":\"arn:aws:ecs:eu-west-1:577770582757:task/48b872ca-a6d6-408c-ab78-c6a9161dc453\",\"networkInterfaces\":[{\"attachmentId\":\"573b2189-8867-4491-88c1-c2843ca7d68d\",\"privateIpv4Address\":\"172.31.19.97\"}]}],\"createdAt\":\"2018-11-25T13:47:11.694Z\",\"launchType\":\"FARGATE\",\"cpu\":\"1024\",\"memory\":\"2048\",\"desiredStatus\":\"STOPPED\",\"group\":\"family:dpnt-coverage-live-python\",\"lastStatus\":\"STOPPED\",\"overrides\":{\"containerOverrides\":[{\"environment\":[{\"name\":\"PARTICIPANT_ID\",\"value\":\"moic01\"},{\"name\":\"REPO\",\"value\":\"s3://tdl-official-videos/FIZ/moic01/sourcecode_20181125T115222.srcs\"},{\"name\":\"CHALLENGE_ID\",\"value\":\"FIZ\"},{\"name\":\"ROUND_ID\",\"value\":\"FIZ_R1\"},{\"name\":\"TAG\",\"value\":\"FIZ_R1/done\"}],\"name\":\"default-container\"}]},\"attachments\":[{\"id\":\"573b2189-8867-4491-88c1-c2843ca7d68d\",\"type\":\"eni\",\"status\":\"DELETED\",\"details\":[{\"name\":\"subnetId\",\"value\":\"subnet-cf2386b9\"},{\"name\":\"networkInterfaceId\",\"value\":\"eni-96fe1ea0\"},{\"name\":\"macAddress\",\"value\":\"06:0f:fb:01:46:e8\"},{\"name\":\"privateIPv4Address\",\"value\":\"172.31.19.97\"}]}],\"connectivity\":\"DISCONNECTED\",\"stoppingAt\":\"2018-11-25T13:48:19.751Z\",\"stoppedAt\":\"2018-11-25T13:48:32.586Z\",\"executionStoppedAt\":\"2018-11-25T13:47:49Z\",\"stoppedReason\":\"Task failed to start\",\"updatedAt\":\"2018-11-25T13:48:32.586Z\",\"taskArn\":\"arn:aws:ecs:eu-west-1:577770582757:task/48b872ca-a6d6-408c-ab78-c6a9161dc453\",\"taskDefinitionArn\":\"arn:aws:ecs:eu-west-1:577770582757:task-definition/dpnt-coverage-live-python:1\",\"version\":5}}";

    @Rule
    public EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private EventsAlertHandler eventsAlertHandler;
    private SqsEventQueue sqsEventQueue;
    private Stack<CoverageProcessingFailedEvent> coverageProcessingFailedEvents;
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

        coverageProcessingFailedEvents = new Stack<>();
        queueEventHandlers.on(CoverageProcessingFailedEvent.class, coverageProcessingFailedEvents::add);

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
    public void when_coverage_processing_fails_in_a_container_an_event_should_flow_to_the_sqs_queue() throws Exception {
        // Given - The participant has commits and pushes code after solving a challenge
        String challengeId = generateId();
        String roundId = generateId();
        String errorMessage = "";

        // When - Coverage processing fails in the container on the ECS
        CoverageProcessingFailedECSEvent ecsEvent = new CoverageProcessingFailedECSEvent(
                ECS_COVERAGE_FAILED_EVENT,
                roundId,
                challengeId,
                errorMessage
        );
        eventsAlertHandler.handleRequest(
                convertToMap(wrapAsSNSEvent(ecsEvent)),
                NO_CONTEXT);

        // Then - a failure event is wrapped and sent to the SQS queue
        waitForQueueToReceiveEvents();
        CoverageProcessingFailedEvent queueEvent = coverageProcessingFailedEvents.pop();
        String eventString = queueEvent.toString();  // eventually might be a idea to verify the event sent getEventAsJsonString();
        assertThat(eventString, allOf(containsString(roundId),
                containsString(challengeId),
                containsString(errorMessage))
        );
    }

    @Test
    public void an_unsupported_ecs_event_should_not_flow_to_the_sqs_queue() {
        // Given - The participant does some other activity while solving a challenge
        String challengeId = generateId();
        String roundId = generateId();
        String errorMessage = "";

        // When - Some unsupported event happens, let's say it's an unsupported ECS event in this case
        CoverageProcessingFailedECSEvent unsupportedECSEvent = new CoverageProcessingFailedECSEvent(
                UNSUPPORTED_ECS_EVENT,
                roundId,
                challengeId,
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

    private String wrapAsSNSEvent(CoverageProcessingFailedECSEvent ecsEvent) throws JsonProcessingException {
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
