#!/bin/bash
set -euxo pipefail

##############################################################################
##
##  Travis CI test script
##
##############################################################################

./scripts/packageApps.sh

mvn -pl system verify
mvn -pl inventory verify
mvn -pl query verify

./scripts/buildImages.sh
./scripts/startContainers.sh

sleep 180

docker logs system1
docker logs system2
docker logs system3
docker logs inventory
docker logs query

systemCPULoad="$(curl --write-out "%{http_code}" --silent --output /dev/null "http://localhost:9080/query/systems")"

if [ "$systemCPULoad" == "200" ]
then
  echo SystemInventory OK
else
  echo System Inventory status:
  echo "$systemCPULoad"
  echo ENDPOINT
  exit 1
fi

./scripts/stopContainers.sh
