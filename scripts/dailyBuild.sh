#!/bin/bash
while getopts t:d:b:u: flag;
do
    case "${flag}" in
        t) DATE="${OPTARG}";;
        d) DRIVER="${OPTARG}";;
        b) BUILD="${OPTARG}";;
        u) DOCKER_USERNAME="${OPTARG}"
    esac
done

echo "Testing daily OpenLiberty image"

sed -i "\#<artifactId>liberty-maven-plugin</artifactId>#a<configuration><install><runtimeUrl>https://public.dhe.ibm.com/ibmdl/export/pub/software/openliberty/runtime/nightly/"$DATE"/"$DRIVER"</runtimeUrl></install></configuration>" system/pom.xml query/pom.xml inventory/pom.xml
cat system/pom.xml query/pom.xml inventory/pom.xml

sed -i "s;FROM openliberty/open-liberty:kernel-java8-openj9-ubi;FROM "$DOCKER_USERNAME"/olguides:"$BUILD";g" system/Dockerfile query/Dockerfile inventory/Dockerfile
cat system/Dockerfile query/Dockerfile inventory/Dockerfile

docker pull $DOCKER_USERNAME"/olguides:"$BUILD

sudo ../scripts/testApp.sh

echo "Testing latest OL Docker image"

sed -i "s;FROM "$DOCKER_USERNAME"/olguides:"$BUILD";FROM openliberty/daily:latest;g" system/Dockerfile query/Dockerfile inventory/Dockerfile

cat system/Dockerfile query/Dockerfile inventory/Dockerfile

docker pull "openliberty/daily:latest"

IMAGEBUILDLEVEL=$(docker inspect --format "{{ index .Config.Labels \"org.opencontainers.image.revision\"}}" openliberty/daily:latest)

if [ $IMAGEBUILDLEVEL == $BUILD ] 
then
    sudo ../scripts/testApp.sh
else
    echo "Image does not match input build level for testing"
fi
