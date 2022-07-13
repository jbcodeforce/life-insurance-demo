#!/bin/sh

if [[ $# -ne 1 ]]
then
  echo "Usage: need the URL of the Kafka connect listener"
  exit 1
fi
echo Let delete existing definition 

set -x
curl -X DELETE  -H 'content-type: application/json' http://$1/connectors/mq-source
echo '\n'
curl -X POST  -H 'content-type: application/json' -d@"./environments/local/kconnect/mq-source.json" http://$1/connectors
echo '\n'
curl   -H 'content-type: application/json' http://$1/connectors
