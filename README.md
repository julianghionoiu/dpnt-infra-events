# dpnt-infra-events

Datapoint processing - infrastruction events

### Updating sub-modules

Root project contains three git submodules:

- local-s3  
- local-sqs 

Run the below command in the project root to update the above submodules:

```
git submodule update --init
```

## Acceptance test

Start external dependencies
```bash
python local-sqs/elasticmq-wrapper.py start
python local-s3/minio-wrapper.py start
minio config host add myminio http://192.168.1.190:9000 local_test_access_key local_test_secret_key
```

Run the acceptance test

```
./gradlew --rerun-tasks test
```

Stop external dependencies
```bash
python local-sqs/elasticmq-wrapper.py stop
python local-s3/minio-wrapper.py stop
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

Create a Minio bucket locally
```
export TEST_S3_LOCATION=./local-s3/.storage/tdl-test-auth/TCH/user01
mkdir -p $TEST_S3_LOCATION
cp src/test/resources/test1.srcs $TEST_S3_LOCATION
echo $TEST_S3_LOCATION && ls -l $TEST_S3_LOCATION
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
```
AWS_ACCESS_KEY_ID=local_test_access_key \
AWS_SECRET_KEY=local_test_secret_key \
SLS_DEBUG=* \
serverless invoke local \
 --function recorder_started \
 --path src/test/resources/tdl/datapoint/infra_events/sample_some_event.json
```

Note: the below can also be used:

```
export AWS_PROFILE=befaster                        # pre-configured profile contained in ~/.aws/credentials
```

instead of setting `AWS_ACCESS_KEY_ID` and `AWS_SECRET_KEY`


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

Create an S3 event json and place it in a temp folder, say `xyz/sample_some_event.json`
Set the bucket and the key to some meaningful values.

Invoke the dev lambda

```
SLS_DEBUG=* serverless invoke --stage dev --function some_lambda --path src/test/resources/tdl/datapoint/infra_events/sample_some_event.json
```

Check the destination queue for that particular environment. Check the ECS Task status and logs

Note: the sample_some_event.json file contains [...something...].
