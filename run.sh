#!/bin/sh

VERSION=`xmllint --xpath "//*[local-name()='project']/*[local-name()='version']/text()" pom.xml`

docker run \
--name notification \
--rm \
-e DW_DATACENTER_ID=1 \
-e DW_WORKER_ID=1 \
-e PORT=8080 \
-p 8080:8080 \
smoketurner/notification:${VERSION}
