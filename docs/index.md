# Life insurance demonstration

This repository includes a simple example of how to integrate with an existing MQ based framework that manage message distribution between MQ applications, in life insurance domain, to plug eventing capabilities like IBM Event Streams (Kafka) with minimum disruption.

## Architecture context

Existing solution integrate a queueing framework that receive message from different applications (front end / mobile for the most part) in a Life insurance domain. This framework helps to support error management, retry, notification, data transformation, data life cycle and governance.

The ask is to see how event-driven architecture will help to support some of the capability of the framework or complement it.

Figure below illustrate a generic view of how the existing framework is running:

![](./images/existing.png)

* At the top we have different front end applications that can send transactional data (write to life insurance model), or not transactional
* The APIs consumed by the front end could be mediated with ESB (IBM IIB) flows, and then some are publishing messages to IBM MQ queues. 
* From those queues, we can get different processing running all together to do data enrichment, transformation, to get subscriber applications consuming those data.
* Other services are responsible to do retries, auditing, manage errors, or notify end user with a mobile push or email back-end 
* An important component of this framework is the transaction event sourcing capability: keep state of change on some interesting transactional data: for example a life insurance offer.

Different flows are doing the needed works, and all this framework is basically supporting long running transaction processing and notification engine.

Also note, that to be generic this framework defines different mesage type (600) and adapt mediation flow via configuration.

The solution in on bare metal or VM running on premise.
## Requirements to demonstrate

* Address how to extend existing architecture with Kafka based middleware and streaming processing. (See [next section](/#example-of-non-desruptive-integration))
* Demonstrate streaming processing with the exactly once delivery
* Ensure Event order is not changed: in the queuing approach with subscription, it is possible that a message arrived after another one could be processed before the first one is completed, which could impact data integrity.
* Demonstrate Data transformation to target different models, to prepare the data for a specific subscriber (a kafka consumer)
* Support message content based routing
* Dead letter queue support for data in error
* Support CloudEvent.io to present metadata around the message
* Support Schema management in registry to control the definition of the message in a unique central repository
* Demonstrate access control to topic

## Example of non-desruptive integration

The existing framework can be extended by adding Kafka MQ source and sink connectors and then deploy Kafka based middleware (IBM Event Streams) so transactional or non-transactional data will be in different Kafka Topic.

![](./images/es-integration.png)


* For the streaming processing, we propose to do data enrichment, data validation to route erranous data to dead-letter-queue, and data transformation to publish to two different topics for downstream subscribers: these will validate content based routing and enrichment, and exactly once delivery with order guarantee.


???- "Read more"
    * To understand Kafka topic - offset [see this note](https://ibm-cloud-architecture.github.io/refarch-eda/technology/kafka-overview/#topics)
    * Kafka [MQ Source connector lab](https://ibm-cloud-architecture.github.io/refarch-eda/use-cases/connect-mq/)
## Domain model

[See the design section](./design/#simple-domain-model-for-client)
## Components 

We will leverage the following IBM Products:

* Event Streams with one cluster definition is in [this eventstreams-dev yaml](https://github.com/jbcodeforce/life-insurance-demo/blob/main/environments/lf-demo/services/ibm-eventstreams/base/eventstreams-dev.yaml)
* MQ broker with AMQP protocol enabled (See [this folder](https://github.com/jbcodeforce/life-insurance-demo/tree/main/environments/lf-demo/services/ibm-mq/base) for deployment example)
* [Kafka Connector](https://github.com/jbcodeforce/life-insurance-demo/tree/main/environments/lf-demo/services/kconnect)
* [Event end point management](https://www.ibm.com/docs/en/cloud-paks/cp-integration/2022.2?topic=integrations-socializing-your-kafka-event-sources)
* [Schema registry](https://ibm.github.io/event-streams/schemas/overview/)

And develop three components:

* A transaction simulator to send data to MQ to support different demonstration goals. The app is done in Java Messaging Service in [lf-tx-simulator folder](https://github.com/jbcodeforce/life-insurance-demo/tree/main/lf-tx-simulator). 
* a Kafka streaming processing using standard Java Kafka Streams API. The application is in [client-event-processing folder](https://github.com/jbcodeforce/life-insurance-demo/tree/main/client-event-processing) 
* Configuration for MQ source connector. The Yaml file is in [environments mq-source folder](https://github.com/jbcodeforce/life-insurance-demo/tree/main/environments/lf-demo/apps/mq-source)


???- "More reading"
    * [Building reactive Java apps with Quarkus and IBM MQ](https://developer.ibm.com/tutorials/mq-building-cloud-native-reactive-java-messaging-applications/)
    * [Quarkus AMQP 1.0 Quickstart](https://quarkus.io/guides/amqp)