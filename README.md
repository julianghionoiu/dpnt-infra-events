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

```
AWS_ACCESS_KEY_ID=local_test_access_key \
AWS_SECRET_KEY=local_test_secret_key \
SLS_DEBUG=* \
serverless invoke local \
 --function recorder-started \
 --path src/test/resources/tdl/datapoint/infra_events/recorder_started_event.json
```

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

## Serverless successful execution: expected results

On successful execution of your serverless function (lambda), you should see the below output:

```

<--- snipped ---> 

{"s3event":{"eventJson":"{\"Records\":[{\"eventVersion\":\"2.1\",\"eventSource\":\"aws:s3\",\"awsRegion\":\"eu-west-2\",\"eventTime\":\"2019-01-11T21:07:25.954Z\",\"eventName\":\"ObjectCreated:Put\",\"userIdentity\":{\"principalId\":\"AWS:577770582757:tdl-live-mani_0110_01\"},\"requestParameters\":{\"sourceIPAddress\":\"91.110.160.204\"},\"responseElements\":{\"x-amz-request-id\":\"E996F81A6364D452\",\"x-amz-id-2\":\"LqI4jhOSDsCnIL/gW/pAFmAkhJbH2WJ+Kc6e16IwhgWorJwPavdfh60AUoNnksSSFMtxmtTV5j8=\"},\"s3\":{\"s3SchemaVersion\":\"1.0\",\"configurationId\":\"RecordingStarted\",\"bucket\":{\"name\":\"tdl-official-videos\",\"ownerIdentity\":{\"principalId\":\"A39KNTXUHOPHA4\"},\"arn\":\"arn:aws:s3:::tdl-official-videos\"},\"object\":{\"key\":\"HLO/mani_0110_01/last_sync_start.txt\",\"size\":24,\"eTag\":\"7065d91c3b36e89dfa23c6e7ce83af1a\",\"sequencer\":\"005C39058DE5F72FEF\"}}}]}","challengeId":"6dc0aeeed0404241abc1709377e23531","participantId":"11684e961893422dbf9a33679bca3b53"}}

Jan 30, 2019 11:12:49 PM tdl.datapoint.infra_events.EventsAlertHandler handleS3Event
INFO: Process S3 event with: S3BucketEvent{eventJson='{"Records":[{"eventVersion":"2.1","eventSource":"aws:s3","awsRegion":"eu-west-2","eventTime":"2019-01-11T21:07:25.954Z","eventName":"ObjectCreated:Put","userIdentity":{"principalId":"AWS:577770582757:tdl-live-mani_0110_01"},"requestParameters":{"sourceIPAddress":"91.110.160.204"},"responseElements":{"x-amz-request-id":"E996F81A6364D452","x-amz-id-2":"LqI4jhOSDsCnIL/gW/pAFmAkhJbH2WJ+Kc6e16IwhgWorJwPavdfh60AUoNnksSSFMtxmtTV5j8="},"s3":{"s3SchemaVersion":"1.0","configurationId":"RecordingStarted","bucket":{"name":"tdl-official-videos","ownerIdentity":{"principalId":"A39KNTXUHOPHA4"},"arn":"arn:aws:s3:::tdl-official-videos"},"object":{"key":"HLO/mani_0110_01/last_sync_start.txt","size":24,"eTag":"7065d91c3b36e89dfa23c6e7ce83af1a","sequencer":"005C39058DE5F72FEF"}}}]}', challengeId='6dc0aeeed0404241abc1709377e23531', participantId='11684e961893422dbf9a33679bca3b53'}

OK

```
