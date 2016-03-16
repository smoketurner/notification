FROM java:openjdk-8-jre-alpine
MAINTAINER Justin Plock <jplock@smoketurner.com>

LABEL name="notification" version="1.1.1-SNAPSHOT"

RUN mkdir -p /opt
WORKDIR /opt
COPY ./notification.jar /opt
COPY ./notification-application/notification.yml /opt
VOLUME ["/opt"]

EXPOSE 8080 8180
ENTRYPOINT ["java", "-d64", "-server", "-jar", "notification.jar"]
CMD ["server", "notification.yml"]
