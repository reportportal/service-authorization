FROM openjdk:8-jre-alpine

LABEL maintainer "Andrei Varabyeu <andrei_varabyeu@epam.com>"
LABEL version="5.0.0-BETA-6"
LABEL description="EPAM Report portal. SSO Authorization Service"

RUN apk --no-cache add unzip openssl

ENV APP_FILE service-authorization-5.0.0-BETA-6
ENV APP_DOWNLOAD_URL https://dl.bintray.com/epam/reportportal/com/epam/reportportal/service-authorization/5.0.0-BETA-6/$APP_FILE.zip
ENV JAVA_OPTS="-Xmx512m -Djava.security.egd=file:/dev/./urandom"
ENV JAVA_APP=/app/app.jar

RUN sh -c "echo $'#!/bin/sh \n\
exec java $JAVA_OPTS -jar $JAVA_APP' > /start.sh && chmod +x /start.sh"

VOLUME /tmp

RUN mkdir /app
RUN wget -O /app/$APP_FILE.zip $APP_DOWNLOAD_URL
RUN unzip /app/$APP_FILE.zip -d /app/ && rm -rf /app/$APP_FILE.zip
RUN mv /app/$APP_FILE.jar $JAVA_APP

EXPOSE 8080
ENTRYPOINT ["/start.sh"]