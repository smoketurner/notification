FROM java:openjdk-8-jre-alpine
MAINTAINER Justin Plock <jplock@smoketurner.com>

ARG VERSION="1.2.1-SNAPSHOT"

LABEL name="notification" version=$VERSION

ENV PORT 8080
ENV M2_HOME /usr/lib/mvn
ENV M2 $M2_HOME/bin
ENV PATH $PATH:$M2_HOME:$M2
ENV DW_DATACENTER_ID 1
ENV DW_WORKER_ID 1

WORKDIR /app
COPY . .

RUN apk add --no-cache curl openjdk8 && \
    curl http://mirrors.sonic.net/apache/maven/maven-3/3.3.9/binaries/apache-maven-3.3.9-bin.tar.gz | tar -zx && \
    mv apache-maven-3.3.9 /usr/lib/mvn && \
    # build the application into a single JAR, including dependencies
    mvn package -Dmaven.javadoc.skip=true -Dmaven.test.skip=true -Dmaven.source.skip=true && \
    rm notification-application/target/original-*.jar && \
    mv notification-application/target/*.jar app.jar && \
    # remove all build artifacts & dependencies, Maven, and the JDK
    rm -rf /root/.m2 && \
    rm -rf /usr/lib/mvn && \
    rm -rf notification-client/target && \
    rm -rf notification-api/target && \
    rm -rf notification-application/target && \
    apk del openjdk8

CMD java -Ddw.server.connector.port=$PORT -jar app.jar server config.yml
