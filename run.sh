#!/bin/sh

VERSION=`xmllint --xpath "//*[local-name()='project']/*[local-name()='version']/text()" pom.xml`

docker run --rm=true -p 8080:8080 smoketurner/notification:${VERSION}
