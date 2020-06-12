#!/bin/bash
set -euxo pipefail

##############################################################################
##
##  Travis CI test script
##
##############################################################################

./scripts/pacakageApps.sh

mvn -pl system verify
mvn -pl inventory verify
mvn -pl gateway verify

./scripts/buildImages.sh
./scripts/startContainers.sh

sleep 180

docker logs system
docker logs inventory
docker logs gateway

systemCPULoad="$(curl --write-out "%{http_code}" --silent --output /dev/null "http://localhost:9080/api/gateway/systems")"

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
