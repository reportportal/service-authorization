FROM frolvlad/alpine-oraclejdk8:slim

MAINTAINER Andrei Varabyeu <andrei_varabyeu@epam.com>
LABEL version="2.7.0-ALPHA-3"
LABEL description="EPAM Report portal. SSO Authorization Service"

RUN apk --no-cache add unzip

ENV APP_FILE service-authorization-2.7.0-ALPHA-3
ENV APP_DOWNLOAD_URL https://dl.bintray.com/epam/reportportal/com/epam/reportportal/service-authorization/2.7.0-ALPHA-3/$APP_FILE.zip

VOLUME /tmp
ADD $APP_DOWNLOAD_URL /app/

RUN unzip /app/$APP_FILE.zip -d /app/ && rm -rf /app/$APP_FILE.zip
RUN mv /app/$APP_FILE.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["java","-Xmx256m","-Djava.security.egd=file:/dev/./urandom","-jar", "/app/app.jar"]