# Pod Central RSS Lambda

## Getting Started

``` bash
docker pull instructure/dynamo-local-admin
docker run -d -p 10500:8000 -it --rm instructure/dynamo-local-admin

DYNAMO_ENDPOINT=http://localhost:10500 mvnw spring-boot:run
```