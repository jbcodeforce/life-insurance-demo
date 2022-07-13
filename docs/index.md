# Life insurance demonstration

This repository includes a simple example of how to integrate with a MQ system processing messages in life insurance domain, and plug eventing into the architecture with minimum disruption.

## Requirements to demonstrate

* Demonstrate streaming processing with the exactly once processing
* Ensure Event order is not changed: in queue with subscription it is possible that a message arrived after another one could be processed before the first one is completed, which could impact data integrity.
* Demonstrate Data transformation to target different model for specific subscriber (a kafka consumer)
* Support message content based routing
* Dead letter queue
* Support CloudEvent.io to present metadata around the message
* Support Schema management in registry


## Domain model

![](./images/lf-model.png)
## Components 


### Transaction Simulator as source to MQ

![](./images/tx-simulator.png) 