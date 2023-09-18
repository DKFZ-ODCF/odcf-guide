FROM openjdk:17-jdk-alpine
RUN apk --no-cache add bash sudo tzdata
RUN cp /usr/share/zoneinfo/Europe/Berlin /etc/localtime
RUN echo "Europe/Berlin" > /etc/timezone
RUN apk --no-cache del tzdata
VOLUME /tmp
ARG JAR_FILE
COPY ${JAR_FILE} app.jar
ENTRYPOINT ["/config/run.sh"]
