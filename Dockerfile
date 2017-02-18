FROM java:openjdk-8-jre-alpine

ARG VERSION="1.2.2-SNAPSHOT"

LABEL name="notification" version=$VERSION

ENV DW_DATACENTER_ID 1
ENV DW_WORKER_ID 1
ENV PORT 8080

RUN apk add --no-cache curl openjdk8="$JAVA_ALPINE_VERSION"

WORKDIR /app

RUN mkdir -p notification-api notification-application notification-client

COPY pom.xml mvnw ./
COPY .mvn ./.mvn/
COPY notification-api/pom.xml ./notification-api/
COPY notification-application/pom.xml ./notification-application/
COPY notification-client/pom.xml ./notification-client/

RUN ./mvnw install

COPY . .

RUN ./mvnw clean package -DskipTests=true -Dmaven.javadoc.skip=true -Dmaven.source.skip=true && \
    rm notification-application/target/original-*.jar && \
    mv notification-application/target/*.jar app.jar && \
    rm -rf /root/.m2 && \
    rm -rf notification-application/target && \
    rm -rf notification-client/target && \
    rm -rf notification-api/target && \
    apk del openjdk8

HEALTHCHECK --interval=10s --timeout=5s CMD curl --fail http://127.0.0.1:8080/admin/healthcheck || exit 1

EXPOSE 8080

ENTRYPOINT ["java", "-d64", "-server", "-jar", "app.jar"]
CMD ["server", "config.yml"]
