#!/bin/bash
set -euxo pipefail

##############################################################################
##
##  Travis CI test script
##
##############################################################################

mvn -q clean package
docker build -t system:1.0-SNAPSHOT system/.
docker build -t inventory:1.0-SNAPSHOT inventory/.
docker build  -t job:1.0-SNAPSHOT job/.
docker build  -t gateway:1.0-SNAPSHOT gateway/.

./scripts/start-app

mvn verify

./scripts/stop-app
