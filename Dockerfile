FROM java:8-jdk
MAINTAINER Justin Plock <jplock@smoketurner.com>

LABEL name="notification" version="1.0.0-SNAPSHOT"

COPY . /src
WORKDIR /src
RUN mvn package
EXPOSE 8080 8180
ENTRYPOINT ["java", "-d64", "-server", "-jar", "notification-application/target/notification-application-1.0.0-SNAPSHOT.jar"]
CMD ["server", "notification-application/notification.yml"]
