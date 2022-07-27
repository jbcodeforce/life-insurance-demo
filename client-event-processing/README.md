# client-event-processing Project

This project is a basic Kafka Streams processing to do the following:

* Get reference data about client categories from categories topic to KTable
* Get transaction for client update from raw-tx topic to KStream, route non validate data to DLQ topic
* Validated data go to enrichment for client category and transformation by flattening the model
* 

* [The full project description is in this page](https://jbcodeforce.github.io/life-insurance-demo/)
## Run locally

* Run unit and functional tests with Test Container and Kafka streams TestDriver

```sh
mvn verify
```

* Build jars and docker images + push to quay.io

```sh
./scripts/buildAll.sh 0.0.1
```

* Run with docker compose with Event Streams images, connectors and simulator

```sh
# under parent folder
docker compose up -d
```

## Notes

* [See test container with Strimzi](https://github.com/strimzi/test-container)
