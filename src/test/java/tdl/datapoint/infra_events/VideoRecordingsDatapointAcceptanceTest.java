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
import tdl.datapoint.infra_events.support.LocalS3Bucket;
import tdl.datapoint.infra_events.support.LocalSQSQueue;
import tdl.datapoint.infra_events.support.S3Event;
import tdl.datapoint.infra_events.support.SNSEvent;
import tdl.datapoint.infra_events.support.TestVideoFile;
import tdl.participant.queue.connector.EventProcessingException;
import tdl.participant.queue.connector.QueueEventHandlers;
import tdl.participant.queue.connector.SqsEventQueue;
import tdl.participant.queue.events.VideoRecorderStartedEvent;

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
import static org.hamcrest.CoreMatchers.endsWith;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;


public class VideoRecordingsDatapointAcceptanceTest {
    private static final Context NO_CONTEXT = null;

    @Rule
    public EnvironmentVariables environmentVariables = new EnvironmentVariables();

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    private VideoRecorderAlertHandler videoRecorderAlertHandler;
    private SqsEventQueue sqsEventQueue;
    private LocalS3Bucket localS3Bucket;
    private Stack<VideoRecorderStartedEvent> videoRecorderStartedEvents;
    private ObjectMapper mapper;
    private TestVideoFile video1 = new TestVideoFile("video-file-1.mp4");
    private TestVideoFile video2 = new TestVideoFile("video-file-2.mp4");

    @Before
    public void setUp() throws EventProcessingException, IOException {
        environmentVariables.set("AWS_ACCESS_KEY_ID","local_test_access_key");
        environmentVariables.set("AWS_SECRET_KEY","local_test_secret_key");
        setEnvFrom(environmentVariables, Paths.get("config", "env.local.yml"));

        localS3Bucket = LocalS3Bucket.createInstance(
                getEnv(ApplicationEnv.S3_ENDPOINT),
                getEnv(ApplicationEnv.S3_REGION));

        sqsEventQueue = LocalSQSQueue.createInstance(
                getEnv(ApplicationEnv.SQS_ENDPOINT),
                getEnv(ApplicationEnv.SQS_REGION),
                getEnv(ApplicationEnv.SQS_QUEUE_URL));

        videoRecorderAlertHandler = new VideoRecorderAlertHandler();

        QueueEventHandlers queueEventHandlers = new QueueEventHandlers();
        videoRecorderStartedEvents = new Stack<>();
        queueEventHandlers.on(VideoRecorderStartedEvent.class, videoRecorderStartedEvents::add);
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
    public void start_video_player() throws Exception {
        // Given - The participant produces Video files while solving a challenge
        String challengeId = generateId();
        String participantId = generateId();
        String s3destination = String.format("%s/%s/file.mp4", challengeId, participantId);

        // When - Upload event happens
        S3Event s3Event1 = localS3Bucket.putObject(video1.asFile(), s3destination);
        videoRecorderAlertHandler.handleRequest(
                convertToMap(wrapAsSNSEvent(s3Event1)),
                NO_CONTEXT);

        // Then - Repo is created with the contents of the SRCS file
        waitForQueueToReceiveEvents();
        VideoRecorderStartedEvent queueEvent1 = videoRecorderStartedEvents.pop();
        String repoUrl1 = queueEvent1.getVideoFileLink();
        assertThat(repoUrl1, allOf(startsWith("file:///"),
                containsString(challengeId),
                endsWith(participantId)));

        // When - Another upload event happens
        S3Event s3Event2 = localS3Bucket.putObject(video2.asFile(), s3destination);
        videoRecorderAlertHandler.handleRequest(
                convertToMap(wrapAsSNSEvent(s3Event2)),
                NO_CONTEXT);

        // Then - The Video file is appended to the repo
        waitForQueueToReceiveEvents();
        VideoRecorderStartedEvent queueEvent2 = videoRecorderStartedEvents.pop();
        String repoUrl2 = queueEvent2.getVideoFileLink();
        assertThat(repoUrl1, equalTo(repoUrl2));
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
