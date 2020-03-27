FROM adoptopenjdk:11-jdk-hotspot-bionic as builder

WORKDIR /usr/src/reportportal

COPY gradle/ ./gradle
COPY gradlew .
RUN ./gradlew wrapper

COPY project-properties.gradle build.gradle gradlew settings.gradle gradle.properties ./
RUN ./gradlew resolveDependencies --stacktrace

COPY . ./
RUN ./gradlew build --stacktrace -P gcp

FROM openjdk:11-jre-slim
LABEL maintainer="Andrei Varabyeu <andrei_varabyeu@epam.com>"

RUN echo '#!/bin/sh \n exec java ${JAVA_OPTS} -jar ${APP_FILE}' > /start.sh && \
    chmod +x /start.sh

ENV JAVA_OPTS="-Xmx1g -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=/tmp -Djava.security.egd=file:/dev/./urandom"
ENV APP_FILE=/app.jar

VOLUME ["/tmp"]

COPY --from=builder /usr/src/reportportal/build/libs/app.jar $APP_FILE

RUN sh -c "touch $APP_FILE"

EXPOSE 8080

ENTRYPOINT ["/start.sh"]
