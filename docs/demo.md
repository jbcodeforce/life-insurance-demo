# Demonstration Script

## Goal

The goal of the demonstration is to send two messages to illustrate client creation and update message in this order. Then each message is enriched and routed to different topics.

The sequence of business operations:

* Bob TheBuilder is a new client so a web app is creating the record via a POST operation to the client management simulator
* The client record is update by adding a beneficiary as spouce
* The email address is changed.
## Local execution demonstration

Under the project starts docker compose:

```sh
docker-compose up -d
```

1. Verify existing topics are empty 


    ```sh
    chrome http://localhost:9000/
    ```

    ![](./images/topics-kafdrop.png)


1. Configure MQ Source connector

    ```sh
    
    ```

## OpenShift Deployment demonstration