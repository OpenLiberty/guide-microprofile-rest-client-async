#!/bin/bash

docker stop system inventory query kafka zookeeper

docker network rm reactive-app