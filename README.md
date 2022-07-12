# A simple demonstration for Life insurance client domain

This repository includes a simple example of how to integrate with a MQ system processing messages in life insurance domain, and plug eventing into the architecture with minimum disruption.

## Requirements to demonstrate

* Demonstrate streaming processing with the exactly once processing
* Ensure Event order is not changed: in queue with subscription it is possible that a message arrived after another one could be processed before the first one is completed, which could impact data integrity.
* Demonstrate Data transformation to target different model for specific subscriber (a kafka consumer)
* Support message content based routing
* Dead letter queue
* Support CloudEvent.io to present metadata around the message
* Support Schema management in registry

## Components of the demonstration

* MQ broker, with generic transaction
* Event Streams, IBM packaging of Kafka, Apicur.io, and Strimzi.
* Event End Point Gateway


## Run demo locally on your laptop

You need docker engine.

```sh
docker-compose up -d
```

## Deploy to an Existing OpenShift with CP4I install

We assume you have already Cloud Pak for Integration installed, and you are already logged to the OpenShift console.

We have a makefile to drive the installation of the demonstration components


```sh
cd environments
make all
```
