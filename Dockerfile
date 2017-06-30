FROM openjdk:8-jre-alpine

LABEL maintainer "Andrei Varabyeu <andrei_varabyeu@epam.com>"
LABEL version="3.0.2-BETA-2"
LABEL description="EPAM Report portal. SSO Authorization Service"

RUN apk --no-cache add unzip openssl

ENV APP_FILE service-authorization-3.0.2-BETA-2
ENV APP_DOWNLOAD_URL https://dl.bintray.com/epam/reportportal/com/epam/reportportal/service-authorization/3.0.2-BETA-2/$APP_FILE.zip
ENV JAVA_OPTS="-Xmx512m"

VOLUME /tmp

RUN mkdir /app
RUN wget -O /app/$APP_FILE.zip $APP_DOWNLOAD_URL
RUN unzip /app/$APP_FILE.zip -d /app/ && rm -rf /app/$APP_FILE.zip
RUN mv /app/$APP_FILE.jar /app/app.jar

EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -Djava.security.egd=file:/dev/./urandom -jar /app/app.jar"]
