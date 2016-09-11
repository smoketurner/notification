#!/bin/sh

VERSION="1.2.1-SNAPSHOT"

docker build --build-arg VERSION=${VERSION} -t "smoketurner/notification:${VERSION}" .
