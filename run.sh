#!/bin/sh

VERSION=`xmllint --xpath "//*[local-name()='project']/*[local-name()='version']/text()" pom.xml`

docker run \
--name notification \
--rm \
-e PORT=8080 \
-p 8080:8080 \
smoketurner/notification:${VERSION}
