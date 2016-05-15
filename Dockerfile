FROM java:openjdk-8-jre-alpine
MAINTAINER Justin Plock <jplock@smoketurner.com>

ARG VERSION="1.2.1-SNAPSHOT"

LABEL name="notification" version=$VERSION

ENV DW_DATACENTER_ID 1
ENV DW_WORKER_ID 1

RUN mkdir -p /opt/notification
WORKDIR /opt/notification
COPY ./notification.jar /opt/notification
COPY ./notification-application/notification.yml /opt/notification
VOLUME ["/opt/notification"]

EXPOSE 8080 8180
ENTRYPOINT ["java", "-d64", "-server", "-jar", "notification.jar"]
CMD ["server", "notification.yml"]
