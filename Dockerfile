FROM java:openjdk-8-jre-alpine
MAINTAINER Justin Plock <jplock@smoketurner.com>

ARG VERSION="1.2.1-SNAPSHOT"

LABEL name="notification" version=$VERSION

ENV DW_DATACENTER_ID 1
ENV DW_WORKER_ID 1
ENV PORT 8080

RUN apk add --no-cache openjdk8="$JAVA_ALPINE_VERSION"

WORKDIR /app

RUN mkdir -p notification-api notification-application notification-client

COPY pom.xml mvnw ./
COPY .mvn ./.mvn/
COPY notification-api/pom.xml ./notification-api/
COPY notification-application/pom.xml ./notification-application/
COPY notification-client/pom.xml ./notification-client/

RUN ./mvnw install

COPY . .

RUN ./mvnw package -DskipTests=true -Dmaven.javadoc.skip=true -Dmaven.source.skip=true && \
    rm notification-application/target/original-*.jar && \
    mv notification-application/target/*.jar app.jar && \
    rm -rf /root/.m2 && \
    rm -rf notification-application/target && \
    rm -rf notification-client/target && \
    rm -rf notification-api/target && \
    apk del openjdk8

ENTRYPOINT ["java", "-d64", "-server", "-jar", "app.jar"]
CMD ["server", "config.yml"]
