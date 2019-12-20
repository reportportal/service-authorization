FROM openjdk:8-jre-alpine

LABEL maintainer="Andrei Varabyeu <andrei_varabyeu@epam.com>"
LABEL version="5.0.0-RC-5"
LABEL description="Unified Authorization Trap for all ReportPortal's Services"

RUN apk --no-cache add ca-certificates openssl

ENV APP_FILE service-authorization-5.0.0-RC-5-exec.jar
ENV APP_DOWNLOAD_URL https://dl.bintray.com/epam/reportportal/com/epam/reportportal/service-authorization/5.0.0-RC-5/$APP_FILE
ENV JAVA_OPTS="-Xmx512m -Djava.security.egd=file:/dev/./urandom"

RUN sh -c "echo $'#!/bin/sh \n\
exec java \$JAVA_OPTS -jar \$APP_FILE' > /start.sh && chmod +x /start.sh"

VOLUME /tmp

RUN wget -O /$APP_FILE $APP_DOWNLOAD_URL

EXPOSE 8080
ENTRYPOINT /start.sh
