# dpnt-infra-events

AWS Events Datapoint processing - infrastructure events

### Updating sub-modules

Root project contains a single git submodule:

- local-sqs 

Run the below command in the project root to update the above submodules:

```
git submodule update --init
```

## Acceptance test

Start external dependencies
```bash
python local-sqs/elasticmq-wrapper.py start
```

Run the acceptance test

```
./gradlew --rerun-tasks test
```

Stop external dependencies
```bash
python local-sqs/elasticmq-wrapper.py stop
```

## Packaging

Install Serverless

Ensure you have new version (v6.4.0) of `npm` installed, installing `serverless` fails with older versions of npm:

```
npm install -g npm         # optional: to get the latest version of npm
npm install -g serverless

serverless info
```

Now, have a look at `serverless.yml`

Create an environment configuration in `./config` by creating copy after `env.local.yml`

## Local testing

Ensure that the three components above i.e. `local-xxx` are running before running the below commands.

Build package
```
./gradlew clean test shadowJar
```

Create config file for respective env profiles:

```
cp config/local.params.yml config/dev.params.yml
```

or

```
cp config/dev.params.yml config/live.params.yml
```


Invoke local function manually
(Ensure you have built the latest version of the artifact and it is present in the `build/libs` folder)

_recorder-started_ event

```
AWS_ACCESS_KEY_ID=local_test_access_key \
AWS_SECRET_KEY=local_test_secret_key \
SLS_DEBUG=* \
serverless invoke local \
 --function recorder-started \
 --path src/test/resources/tdl/datapoint/infra_events/recorder_started_event.json
```

See at the bottom of the README for other events.

Note: the below can also be used:

```
export AWS_PROFILE=befaster                        # pre-configured profile contained in ~/.aws/credentials
```

instead of setting `AWS_ACCESS_KEY_ID` and `AWS_SECRET_KEY`

See section [Serverless successful execution: expected results]() for the expected result from the above successful execution. 

## Remote deployment

Build package
```
./gradlew clean test shadowJar
```

Deploy to DEV
```
serverless deploy --stage dev
```

Deploy to LIVE
```
serverless deploy --stage live
```

## Remote testing

Create an AWS event json and place it in a temp folder, say `xyz/sample_some_event.json`

Invoke the dev lambda
(Ensure you have built the latest version of the artifact and it is present in the `build/libs` folder)

```
SLS_DEBUG=* serverless invoke --stage dev --function recorder-started \
                       --path src/test/resources/tdl/datapoint/infra_events/recorder_started_event.json
```

Check the destination queue for that particular environment. Check the ECS Task status and logs

See the next section to see the expected result. 

Note: the `recorder_started_event.json` file contains an s3 action event, challenge id and participant id.

## Running other serverless events locally 

_video-processing-failed_ event

```
AWS_ACCESS_KEY_ID=local_test_access_key \
AWS_SECRET_KEY=local_test_secret_key \
SLS_DEBUG=* \
serverless invoke local \
 --function ecs-video-processing-failed \
 --path src/test/resources/tdl/datapoint/infra_events/video_processing_failed_event.json
```

_coverage-processing-failed_ event

```
AWS_ACCESS_KEY_ID=local_test_access_key \
AWS_SECRET_KEY=local_test_secret_key \
SLS_DEBUG=* \
serverless invoke local \
 --function ecs-coverage-processing-failed \
 --path src/test/resources/tdl/datapoint/infra_events/coverage_processing_failed_event.json
```

## Serverless successful execution: expected results

On successful execution of your serverless function (lambda), you should see the below output:

_recorder-started_ event

```
<--- snipped ---> 

{"s3event":{"eventJson":"{\"Records\":[{\"eventVersion\":\"2.1\",\"eventSource\":\"aws:s3\",\"awsRegion\":\"eu-west-2\",\"eventTime\":\"2019-01-11T21:07:25.954Z\",\"eventName\":\"ObjectCreated:Put\",\"userIdentity\":{\"principalId\":\"AWS:577770582757:tdl-live-mani_0110_01\"},\"requestParameters\":{\"sourceIPAddress\":\"91.110.160.204\"},\"responseElements\":{\"x-amz-request-id\":\"E996F81A6364D452\",\"x-amz-id-2\":\"LqI4jhOSDsCnIL/gW/pAFmAkhJbH2WJ+Kc6e16IwhgWorJwPavdfh60AUoNnksSSFMtxmtTV5j8=\"},\"s3\":{\"s3SchemaVersion\":\"1.0\",\"configurationId\":\"RecordingStarted\",\"bucket\":{\"name\":\"tdl-official-videos\",\"ownerIdentity\":{\"principalId\":\"A39KNTXUHOPHA4\"},\"arn\":\"arn:aws:s3:::tdl-official-videos\"},\"object\":{\"key\":\"HLO/mani_0110_01/last_sync_start.txt\",\"size\":24,\"eTag\":\"7065d91c3b36e89dfa23c6e7ce83af1a\",\"sequencer\":\"005C39058DE5F72FEF\"}}}]}","challengeId":"6dc0aeeed0404241abc1709377e23531","participantId":"11684e961893422dbf9a33679bca3b53"}}

Jan 30, 2019 11:12:49 PM tdl.datapoint.infra_events.EventsAlertHandler handleS3Event
INFO: Process S3 event with: S3BucketEvent{eventJson='{"Records":[{"eventVersion":"2.1","eventSource":"aws:s3","awsRegion":"eu-west-2","eventTime":"2019-01-11T21:07:25.954Z","eventName":"ObjectCreated:Put","userIdentity":{"principalId":"AWS:577770582757:tdl-live-mani_0110_01"},"requestParameters":{"sourceIPAddress":"91.110.160.204"},"responseElements":{"x-amz-request-id":"E996F81A6364D452","x-amz-id-2":"LqI4jhOSDsCnIL/gW/pAFmAkhJbH2WJ+Kc6e16IwhgWorJwPavdfh60AUoNnksSSFMtxmtTV5j8="},"s3":{"s3SchemaVersion":"1.0","configurationId":"RecordingStarted","bucket":{"name":"tdl-official-videos","ownerIdentity":{"principalId":"A39KNTXUHOPHA4"},"arn":"arn:aws:s3:::tdl-official-videos"},"object":{"key":"HLO/mani_0110_01/last_sync_start.txt","size":24,"eTag":"7065d91c3b36e89dfa23c6e7ce83af1a","sequencer":"005C39058DE5F72FEF"}}}]}', challengeId='6dc0aeeed0404241abc1709377e23531', participantId='11684e961893422dbf9a33679bca3b53'}

OK

```

_video-processing-failed_ event

```
<--- snipped ---> 

Jan 31, 2019 12:39:22 AM tdl.datapoint.infra_events.EventsAlertHandler handleECSEvent
INFO: Process ECS event with: ECSEvent{eventJson='{"version":"0","id":"305f07f3-c812-e2a5-e7fa-626f6ac46ce1","detail-type":"ECS Task State Change","source":"aws.ecs","account":"577770582757","time":"2018-09-02T16:14:16Z","region":"eu-west-1","resources":["arn:aws:ecs:eu-west-1:577770582757:task/817dcb71-de09-4b7c-af12-b73eb7699bff"],"detail":{"clusterArn":"arn:aws:ecs:eu-west-1:577770582757:cluster/dpnt-coverage-live","containerInstanceArn":"arn:aws:ecs:eu-west-1:577770582757:container-instance/c8291511-dfc2-4b19-b7cb-496b59a6becf","containers":[{"containerArn":"arn:aws:ecs:eu-west-1:577770582757:container/72c5f96c-6e65-4528-b42d-3cf3e52f74d8","exitCode":255,"lastStatus":"STOPPED","name":"default-container","taskArn":"arn:aws:ecs:eu-west-1:577770582757:task/817dcb71-de09-4b7c-af12-b73eb7699bff","networkInterfaces":[]}],"createdAt":"2018-09-02T16:11:26.191Z","launchType":"FARGATE","cpu":"1024","memory":"2048","desiredStatus":"STOPPED","group":"family:dpnt-coverage-live-nodejs","lastStatus":"STOPPED","overrides":{"containerOverrides":[{"environment":[{"name":"PARTICIPANT_ID","value":"hyks01"},{"name":"REPO","value":"s3://tdl-official-videos/CHK/hyks01/sourcecode_20180902T113408.srcs"},{"name":"CHALLENGE_ID","value":"CHK"},{"name":"ROUND_ID","value":"CHK_R2"},{"name":"TAG","value":"CHK_R2/done"}],"name":"default-container"}]},"connectivity":"CONNECTED","pullStartedAt":"2018-09-02T16:11:37.959Z","startedAt":"2018-09-02T16:12:57.959Z","stoppingAt":"2018-09-02T16:14:03.598Z","stoppedAt":"2018-09-02T16:14:16.312Z","pullStoppedAt":"2018-09-02T16:12:57.959Z","executionStoppedAt":"2018-09-02T16:14:03Z","stoppedReason":"Essential container in task exited","updatedAt":"2018-09-02T16:14:16.312Z","taskArn":"arn:aws:ecs:eu-west-1:577770582757:task/817dcb71-de09-4b7c-af12-b73eb7699bff","taskDefinitionArn":"arn:aws:ecs:eu-west-1:577770582757:task-definition/dpnt-coverage-live-nodejs:1","version":5}}', challengeId='141873a7aa854caa84aac3e98af72696', participantId='46ff08a1a9294fcd933d0c2c063c743d'}

OK
```

_coverage-processing-failed_ event

```
<--- snipped ---> 

Feb 04, 2019 3:44:56 PM tdl.datapoint.infra_events.EventsAlertHandler handleECSCoverageEvent
INFO: Process ECS Coverage Processing Failure event with: CoverageProcessingFailedECSEvent{eventJson='{"version":"0","id":"30ca1d6a-80b8-ecf9-4cdc-3531554d407a","detail-type":"ECS Task State Change","source":"aws.ecs","account":"577770582757","time":"2018-11-25T13:48:32Z","region":"eu-west-1","resources":["arn:aws:ecs:eu-west-1:577770582757:task/48b872ca-a6d6-408c-ab78-c6a9161dc453"],"detail":{"clusterArn":"arn:aws:ecs:eu-west-1:577770582757:cluster/dpnt-coverage-live","containers":[{"containerArn":"arn:aws:ecs:eu-west-1:577770582757:container/2d73eb67-2216-4202-b008-27ebba56c1be","lastStatus":"STOPPED","name":"default-container","taskArn":"arn:aws:ecs:eu-west-1:577770582757:task/48b872ca-a6d6-408c-ab78-c6a9161dc453","networkInterfaces":[{"attachmentId":"573b2189-8867-4491-88c1-c2843ca7d68d","privateIpv4Address":"172.31.19.97"}]}],"createdAt":"2018-11-25T13:47:11.694Z","launchType":"FARGATE","cpu":"1024","memory":"2048","desiredStatus":"STOPPED","group":"family:dpnt-coverage-live-python","lastStatus":"STOPPED","overrides":{"containerOverrides":[{"environment":[{"name":"PARTICIPANT_ID","value":"moic01"},{"name":"REPO","value":"s3://tdl-official-videos/FIZ/moic01/sourcecode_20181125T115222.srcs"},{"name":"CHALLENGE_ID","value":"FIZ"},{"name":"ROUND_ID","value":"FIZ_R1"},{"name":"TAG","value":"FIZ_R1/done"}],"name":"default-container"}]},"attachments":[{"id":"573b2189-8867-4491-88c1-c2843ca7d68d","type":"eni","status":"DELETED","details":[{"name":"subnetId","value":"subnet-cf2386b9"},{"name":"networkInterfaceId","value":"eni-96fe1ea0"},{"name":"macAddress","value":"06:0f:fb:01:46:e8"},{"name":"privateIPv4Address","value":"172.31.19.97"}]}],"connectivity":"DISCONNECTED","stoppingAt":"2018-11-25T13:48:19.751Z","stoppedAt":"2018-11-25T13:48:32.586Z","executionStoppedAt":"2018-11-25T13:47:49Z","stoppedReason":"Task failed to start","updatedAt":"2018-11-25T13:48:32.586Z","taskArn":"arn:aws:ecs:eu-west-1:577770582757:task/48b872ca-a6d6-408c-ab78-c6a9161dc453","taskDefinitionArn":"arn:aws:ecs:eu-west-1:577770582757:task-definition/dpnt-coverage-live-python:1","version":5}}', roundId='e592e3145a6442ad89147bdd7c2dcfef', participantId='6ad13e27e20749f798fcd7a7459533f2', errorMessage=''}

OK
```
