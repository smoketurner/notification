FROM maven:3-jdk-8
MAINTAINER Justin Plock <jplock@smoketurner.com>

LABEL name="notification" version="1.0.1-SNAPSHOT"

RUN mkdir -p /src
WORKDIR /src
ADD . /src
RUN mvn package -DskipTests=true && rm -rf $HOME/.m2
WORKDIR notification-application
VOLUME ["/src/notification-application"]

EXPOSE 8080 8180
ENTRYPOINT ["java", "-d64", "-server", "-jar", "target/notification-application-1.0.1-SNAPSHOT.jar"]
CMD ["server", "notification.yml"]