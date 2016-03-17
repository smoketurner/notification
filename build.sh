#!/bin/sh

VERSION="1.1.1-SNAPSHOT"

docker rm -f build-cont
docker build -t build-img -f Dockerfile.build .
docker create --name build-cont build-img
docker cp "build-cont:/src/notification-application/target/notification-application-${VERSION}.jar" ./notification.jar
docker build -t "smoketurner/notification:${VERSION}" .
