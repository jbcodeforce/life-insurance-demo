#!/bin/sh

scriptDir="$( cd "$( dirname "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )"

URL=localhost:8083
echo Let delete existing definition 

set -x
curl -X DELETE  -w "%{http_code}" -H 'content-type: application/json' http://$URL/connectors/mq-source
echo '\n'
curl -X POST  -w "%{http_code}" -H 'content-type: application/json' -d@"$scriptDir/kconnect/mq-source.json" http://$URL/connectors
echo '\n'
curl  -w "%{http_code}" -H 'content-type: application/json' http://$URL/connectors
