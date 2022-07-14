# [A simple demonstration for Life insurance client domain](https://jbcodeforce.github.io/life-insurance-demo/)

This repository includes a simple example of how to integrate with an existing MQ based framework that manage message distribution between MQ applications, in life insurance domain, to plug eventing capabilities like IBM Event Streams (Kafka) with minimum disruption.

[Read in mkdocs book](https://jbcodeforce.github.io/life-insurance-demo/)

The repository includes different sub folder for each components, and an environment folder for OpenShift deployments.


## Components of the demonstration

* MQ broker, with generic transaction
* Event Streams, IBM packaging of Kafka, Apicur.io, and Strimzi.
* Event End Point Gateway
* A Transaction simulator to post to MQ using Reactive Messaging and AMQP

