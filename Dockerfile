FROM openjdk:8-jre-alpine

LABEL maintainer="Andrei Varabyeu <andrei_varabyeu@epam.com>"
LABEL version="5.0.0-RC-5"
LABEL description="Unified Authorization Trap for all ReportPortal's Services"

RUN apk --no-cache add ca-certificates unzip openssl

ENV APP_FILE service-authorization-5.0.0-RC-5
ENV APP_DOWNLOAD_URL https://dl.bintray.com/epam/reportportal/com/epam/reportportal/service-authorization/5.0.0-RC-5/$APP_FILE.zip
ENV JAVA_OPTS="-Xmx512m -Djava.security.egd=file:/dev/./urandom"
ENV JAVA_APP=/app/app.jar

RUN sh -c "echo $'#!/bin/sh \n\
exec java \$JAVA_OPTS -jar \$JAVA_APP' > /start.sh && chmod +x /start.sh"

VOLUME /tmp

RUN mkdir /app && \
    wget -O /app/$APP_FILE.zip $APP_DOWNLOAD_URL && \
    unzip /app/$APP_FILE.zip -d /app/ && rm -rf /app/$APP_FILE.zip && \
    mv /app/$APP_FILE.jar $JAVA_APP

EXPOSE 8080
ENTRYPOINT /start.sh
