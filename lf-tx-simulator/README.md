# lf-tx-simulator Project

This project simulate life insurance message to MQ using AMQP and reactive messaging.

See more explanation in [this ibm tutorial](https://developer.ibm.com/tutorials/mq-building-cloud-native-reactive-java-messaging-applications/) and [base quarkus code]()

## Function to support

* Expose a REST API to post new insured person
* Expose a REST API to put to update an existing insured person
* Expose an API to get Client Categories
* Expose API to get relation types
* Support SwaggerUI and health APIs
* Generate a Transaction message to MQ via AMQP protocol
* Support dynamic configuration of target queue.
## Running the application in dev mode

You can run your application in dev mode that enables live coding using:

```shell script
docker compose up -d
./mvnw compile quarkus:dev
```

> **_NOTE:_**  Quarkus Dev UI,is at http://localhost:8080/q/dev/.  Swagger Quarkus Dev UI,is at http://localhost:8080/q/swagger-ui

* Use the e2e script to send a new client to the REST end point, and see the message in DEV.QUEUE.1 in the MQ console at 
https://localhost:9443/  admin  passw0rd
## Packaging and running the application

The application can be packaged using:

```shell script
./script/buildAll.sh
```

It produces the `quarkus-run.jar` file in the `target/quarkus-app/` directory, build a docker image and push it to quay.io

