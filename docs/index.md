# Life insurance demonstration

This repository includes a simple example of how to integrate, with minimum disruption, IBM Event Streams (Kafka) with an existing MQ based framework which manages messages distribution between MQ applications. The example is in life insurance domain but could be applied in other industry. 

## Architecture context

The existing solution integrate a queueing framework that receives messages from different applications (front end / mobile for the most part) and addresses message distribution, message transformation, error management, retry, notification, data life cycle auditing and governance.

The ask is to see how event-driven architecture will help to support some of the capability of the framework or complement it, and how it can help propagate message as events to event-driven microservices.

Figure below illustrates a generic view of how the existing framework is running:

![](./images/existing.png)

* At the top we have different front end applications that can send transactional data (write to life insurance model), or not transactional ones
* The APIs consumed by the frontend could be mediated with ESB (IBM IIB) flows, and then some of those flows are publishing messages to IBM MQ queues. 
* From those queues, we can get different processing running all together to do data enrichment, transformation, to get subscriber applications consuming those data.
* Other services are responsible to do retries, auditing, manage errors, or notify end user with a mobile push or email back-end 
* An important component of this framework is the transaction event sourcing capability: keep state of change on some interesting transactional data: for example a life insurance offer.

Different flows are doing the needed works, and all this framework is basically supporting long running transaction processing and a notification engine.

Also note, that to be generic this framework defines different message types (600) and adapt mediation flow via configuration.

The solution in on bare metal or VM running on premise.
## Requirements to demonstrate

1. Address how to **extend existing** architecture with Kafka based middleware and streaming processing. (See [next section](/#example-of-non-desruptive-integration))
1. Demonstrate **streaming processing** with the exactly once delivery (See [transaction streaming component](./design/#the-client-event-stream-processing))
1. Ensure Event order is not changed: in the queuing approach with subscription, it is possible that a message arrived after another one could be processed before the first one is completed, which could impact data integrity.
1. Demonstrate Data transformation to target different models, to prepare the data for a specific subscriber (a kafka consumer). ([See this streaming code](https://github.com/jbcodeforce/life-insurance-demo/blob/5902d817e1ad2b47880df2988e384b6bec5470b0/client-event-processing/src/main/java/org/acme/domain/TopologyProducer.java#L97)) 
1. Support message content based routing
1. Dead letter queue support for data in error
1. Support [CloudEvent.io](http://cloudevent.io/) to present metadata around the message
1. Support Schema management in registry to control the definition of the message in a unique central repository
1. Demonstrate access control to topic [See user declaration](https://github.com/jbcodeforce/life-insurance-demo/blob/main/environments/lf-demo/services/ibm-eventstreams/base/tls-user.yaml). 

## Example of non-desruptive integration

The existing framework can be extended by adding Kafka MQ source and sink connectors in parallel of existing framework, with deploy Kafka based middleware (IBM Event Streams): transactional or non-transactional data will injected from queues to different Kafka Topics as event ready to be processed as soon as created.

![](./images/es-integration.png)


* For the streaming processing, we propose to do data enrichment, data validation to route erranous data to **dead-letter-queue** topic, and data transformation to publish to two different topics for downstream subscribers: these will validate content based routing and enrichment, and exactly once delivery with order guarantee.
* Subscriber applications illustrate in the figure above could be new applications, or existing one connected to Kafka directly or if actually connected to Queue, then those subscribers will be MQ Sink kafka connector.

???- "Read more"
    * To understand Kafka topic - offset [see this note](https://ibm-cloud-architecture.github.io/refarch-eda/technology/kafka-overview/#topics)
    * Kafka [MQ Source connector lab](https://ibm-cloud-architecture.github.io/refarch-eda/use-cases/connect-mq/)
    * [Dead letter queue pattern](https://ibm-cloud-architecture.github.io/refarch-eda/patterns/dlq/)
## Domain model

We pick up the life insurance domain, but as of now very limited, it could be extended in the future. [See the design section](./design/#simple-domain-model-for-client)
## Components 

We leverage the following IBM Products:

* Event Streams with one cluster definition is in [this eventstreams-dev yaml](https://github.com/jbcodeforce/life-insurance-demo/blob/main/environments/lf-demo/services/ibm-eventstreams/base/eventstreams-dev.yaml)
* MQ broker with AMQP protocol enabled (See [this folder](https://github.com/jbcodeforce/life-insurance-demo/tree/main/environments/lf-demo/services/ibm-mq/base) for deployment example)
* [Kafka Connector](https://github.com/jbcodeforce/life-insurance-demo/tree/main/environments/lf-demo/services/kconnect)
* [Event end point management](https://www.ibm.com/docs/en/cloud-paks/cp-integration/2022.2?topic=integrations-socializing-your-kafka-event-sources)
* [Schema registry](https://ibm.github.io/event-streams/schemas/overview/)

And develop three components to demonstrate how to support requirements:

* A transaction simulator to send data to MQ to support different demonstration goals. The app is done in Java Messaging Service in [lf-tx-simulator folder](https://github.com/jbcodeforce/life-insurance-demo/tree/main/lf-tx-simulator). This application also send categories at startup time.
* a Kafka streams processing app which is using standard Java Kafka Streams API. The application is in [client-event-processing folder](https://github.com/jbcodeforce/life-insurance-demo/tree/main/client-event-processing) 
* Configuration for MQ source connector. The Yaml file is in [environments mq-source folder](https://github.com/jbcodeforce/life-insurance-demo/tree/main/environments/lf-demo/apps/mq-source)


[>>> Next: Read more why EDA], fit for purpose(./eda.md)

???+ "More reading"
    * [Building reactive Java apps with Quarkus and IBM MQ](https://developer.ibm.com/tutorials/mq-building-cloud-native-reactive-java-messaging-applications/)
    * [Kafka Streams](https://ibm-cloud-architecture.github.io/refarch-eda/technology/kafka-streams/) with a [beginner guider](https://ibm-cloud-architecture.github.io/eda-tech-academy/lab2/kstream/)
    * [Quarkus AMQP 1.0 Quickstart](https://quarkus.io/guides/amqp)