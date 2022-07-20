
URL=http://localhost:8080

curl -v -X POST $URL/api/v1/clients  -H 'Content-Type: application/json'  -H 'Accept: application/json' -d@$1 