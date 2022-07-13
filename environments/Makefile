
# Setting global variables

DEV_NS=lf-demo
ES_VERSION=v11.0


# -------------------------------------------------------
# Entry points
# ------------------------------------------------------- 

all: create_ns
	@oc project $$DEV_NS 
	eventstreams_config
	mq_config
	kafka_connect
	start_mq_source

create_ns:
	@oc apply -k ./lf-demo/env/overlays

eventstreams_config:
	@oc apply -k ./lf-demo/services/ibm-eventstreams/overlays

mq_config:
	@oc apply -k ./lf-demo/services/ibm-mq/overlays

kafka_connect:
	@oc apply -k ./lf-demo/services/kconnect -n lf-demo

# ----------- app specific -------------
start_mq_source:
	@oc apply -f ./lf-demo/apps/mq-source/kafka-mq-src-connector.yaml -n $(DEV_NS)

apps: tx_simulator

tx_simulator:
	@oc apply -k ./lf-demo/apps/lf-tx-simulator/overlays

clean:  clean_jobs 
	@oc delete -k ./lf-demo/services/ibm-mq/overlays
	@oc delete -k ./lf-demo/services/ibm-eventstreams/overlays
	@oc delete -k ./lf-demo/apps/lf-tx_simulator/overlays

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

