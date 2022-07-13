# [A simple demonstration for Life insurance client domain](https://jbcodeforce.github.io/life-insurance-demo/)

This repository includes a simple example of how to integrate with an existing MQ based framework that manage message distribution between MQ applications, in life insurance domain, to plug eventing capabilities like IBM Event Streams (Kafka) with minimum disruption.

[Read in mkdocs book](https://jbcodeforce.github.io/life-insurance-demo/)

The repository includes different sub folder for each components, and an environment folder for OpenShift deployments.


## Components of the demonstration

* MQ broker, with generic transaction
* Event Streams, IBM packaging of Kafka, Apicur.io, and Strimzi.
* Event End Point Gateway
* A Transaction simulator to post to MQ using Reactive Messaging and AMQP

## Run demo locally on your laptop

You need docker engine.

```sh
docker-compose up -d
```

 1. Go to the simulator UI

        ```sh
        chrome http://localhost:8080/
        ```

1. Go to IBM MQ console to verify messages in Queue, with user: admin/passw0rd

    ```sh
        chrome https://localhost:9443  
    ```

    You may not see the messages if they were already consumed by the Kafka Connector

1. Verify messages are in the items topics

        ```sh
    chrome http://localhost:9000/
    ```

## Deploy to an Existing OpenShift with CP4I install

We assume you have already Cloud Pak for Integration installed, and you are already logged to the OpenShift console.

We have a makefile to drive the installation of the demonstration components


```sh
cd environments
make all
```
