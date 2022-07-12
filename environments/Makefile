
# Setting global variables

DEV_NS=lf-demo
ES_VERSION=v11.0


# -------------------------------------------------------
# Entry points
# ------------------------------------------------------- 

all: create_ns
	@oc project $$DEV_NS 
	eventstreams_config

create_ns:
	@oc apply -k ./lf-demo/env/overlays

eventstreams_config:
	@oc apply -k ./lf-demo/services/ibm-eventstreams/overlays

mq_config:
	@oc apply -k ./lf-demo/services/ibm-mq/overlays

kafka_connect:
	@oc apply -k ./lf-demo/services/kconnect/overlays -n lf-demo

# ----------- app specific -------------
start_mq_source:
	@oc apply -f ./lf-demo/apps/mq-source/kafka-mq-src-connector.yaml -n $(DEV_NS)



clean:  clean_jobs 
	@oc delete -k ./lf-demo/services/ibm-mq/overlays
	@oc delete -k ./lf-demo/services/ibm-eventstreams/overlays

clean_jobs:
	@oc delete job cpsecret 
	@oc delete job cp-ca-secret 
	@oc delete job cp-tls-usr-secret

output_details:
	@echo "Install complete.\n\n"
	@echo "Openshift admin credential"
	@oc extract secret/openshift-gitops-cluster -n openshift-gitops --to=-
	@echo "\nMQ Console console url"
	@oc get queuemanager store-mq -o jsonpath='{.status.adminUiUrl}'  -n $(DEV_NS)
	@echo "\n\nEvent Streams Console console url"
	@oc get route dev-ibm-es-ui -o jsonpath='{.status.ingress[].host}'  -n $(DEV_NS)
	@echo "\n\nStore simulator url\n"
	@oc get route store-simulator -o jsonpath='{.status.ingress[].host}'  -n $(DEV_NS)

