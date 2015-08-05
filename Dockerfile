FROM maven:3-jdk-8-onbuild
MAINTAINER Justin Plock <jplock@smoketurner.com>

LABEL name="notification" version="1.0.1-SNAPSHOT"

EXPOSE 8080 8180
ENTRYPOINT ["java", "-d64", "-server", "-jar", "notification-application/target/notification-application-1.0.1-SNAPSHOT.jar"]
CMD ["server", "notification-application/notification.yml"]
