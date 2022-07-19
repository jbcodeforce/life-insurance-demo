
# Setting global variables

DEV_NS=lf-demo
ES_VERSION=v11.0


# -------------------------------------------------------
# Entry points
# ------------------------------------------------------- 

all: create_ns change_ns eventstreams_config mq_config 	kafka_connect 	start_mq_source 	apps

create_ns:
	@oc apply -k ./lf-demo/env/overlays
	@echo "\n---------------------------"
	@echo "ls-demo project created.\n\n"

change_ns:
	@oc project $$DEV_NS 

eventstreams_config:
	@echo "\n------------------------------------------------"
	@echo "Create dev Event Streams cluster in lf-demo project"
	@echo "------------------------------------------------\n"
	@oc apply -k ./lf-demo/services/ibm-eventstreams/overlays


mq_config:
	@echo "\n------------------------------------------------\n"
	@echo "Create dev IBM MQ  in lf-demo project\n"
	@echo "\n------------------------------------------------\n"
	@oc apply -k ./lf-demo/services/ibm-mq/overlays


kafka_connect:
	@echo "\n------------------------------------------------"
	@echo "Create dev Kafka Connect cluster in lf-demo project"
	@echo "\n------------------------------------------------\n"
	@oc apply -k ./lf-demo/services/kconnect -n lf-demo

# ----------- app specific -------------
start_mq_source:
	@echo "\n------------------------------------------------"
	@echo "Deploy MQ Source connector"
	@echo "\n------------------------------------------------\n"
	@oc apply -f ./lf-demo/apps/mq-source/kafka-mq-src-connector.yaml -n $(DEV_NS)

apps: tx_simulator tx_client-agent

tx_simulator:
	@oc apply -k ./lf-demo/apps/lf-tx-simulator/overlays

tx_client-agent:
	@oc apply -k ./lf-demo/apps/lf-client-agent/overlays

clean:  clean_jobs 
	@oc delete -k ./lf-demo/apps/lf-tx-simulator/overlays
	@oc delete -f ./lf-demo/apps/mq-source/kafka-mq-src-connector.yaml
	@oc delete -f ./lf-demo/services/kconnect/kafka-connect.yaml
	@oc delete -k ./lf-demo/services/ibm-mq/overlays
	@oc delete -k ./lf-demo/services/ibm-eventstreams/overlays



clean_jobs:
	@oc delete job cpsecret 


output_details:
	@echo "Install complete.\n\n"
	@echo "Openshift admin credential"
	@oc extract secret/openshift-gitops-cluster -n openshift-gitops --to=-
	@echo "\nMQ Console console url"
	@oc get route ibmmq-route -o jsonpath='{.status.ingress[].host}'  -n $(DEV_NS)
	@echo "\n\nEvent Streams Console console url"
	@oc get route dev-ibm-es-ui -o jsonpath='{.status.ingress[].host}'  -n $(DEV_NS)
	@echo "\n\nTransaction simulator url\n"
	@oc get route lf-tx-simulator -o jsonpath='{.status.ingress[].host}'  -n $(DEV_NS)

