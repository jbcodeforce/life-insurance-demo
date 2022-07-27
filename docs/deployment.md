# OpenShift Deployment


We assume you have already Cloud Pak for Integration installed, and you are already logged to the OpenShift console. 

## Pre-requisites

* make tool
* oc CLI
* Logged to your OpenShift cluster

## Using make

We have a makefile to drive the installation of the demonstration components

```sh
cd environments
make all
```

Example of output

```sh
namespace/lf-demo created
serviceaccount/lf-demo-sa created
clusterrolebinding.rbac.authorization.k8s.io/secrets-to-sa created
job.batch/cpsecret created

---------------------------
ls-demo project created.


Using project "lf-demo" on server "https://api.ahsoka.coc-ibm.com:6443".
Using project "lf-demo" on server "https://api.ahsoka.coc-ibm.com:6443".

------------------------------------------------
Create dev Event Streams cluster in lf-demo project
------------------------------------------------

eventstreams.eventstreams.ibm.com/dev created
kafkatopic.eventstreams.ibm.com/lf-raw-tx created
kafkatopic.eventstreams.ibm.com/lf-tx-a created
kafkatopic.eventstreams.ibm.com/lf-tx-b created
kafkauser.eventstreams.ibm.com/scram-user created
kafkauser.eventstreams.ibm.com/tls-user created

------------------------------------------------

Create dev IBM MQ  in lf-demo project


------------------------------------------------

configmap/mq-config created
configmap/mq-mqsc-config created
queuemanager.mq.ibm.com/lf-demo-mq created

------------------------------------------------
Create dev Kafka Connect cluster in lf-demo project
------------------------------------------------

kafkaconnect.eventstreams.ibm.com/eda-kconnect-cluster created

------------------------------------------------
Deploy MQ Source connector
------------------------------------------------
kafkaconnector.eventstreams.ibm.com/mq-source created
configmap/lf-tx-simulator-cm created
service/lf-tx-simulator created
deployment.apps/lf-tx-simulator created
route.route.openshift.io/lf-tx-simulator created
configmap/lf-client-agent-cm created
service/lf-client-agent created
deployment.apps/lf-client-agent created
route.route.openshift.io/lf-client-agent created
```

