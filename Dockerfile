FROM --platform=$BUILDPLATFORM gradle:8.10.0-jdk21-alpine AS build
ARG RELEASE_MODE
ARG APP_VERSION
WORKDIR /usr/app
COPY . /usr/app
RUN if [ "${RELEASE_MODE}" = true ]; then \
    gradle build --no-build-cache --exclude-task test \
        -PreleaseMode=true \
        -Dorg.gradle.project.version=${APP_VERSION}; \
    else gradle build --no-build-cache --exclude-task test -Dorg.gradle.project.version=${APP_VERSION}; fi

FROM amazoncorretto:21.0.7
LABEL version=${APP_VERSION} description="EPAM ReportPortal. Auth Service" maintainer="Andrei Varabyeu <andrei_varabyeu@epam.com>, Hleb Kanonik <hleb_kanonik@epam.com>"
ARG APP_VERSION=${APP_VERSION}
ENV APP_DIR=/usr/app
ENV JAVA_OPTS="-Xmx1g -XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=70 -Djava.security.egd=file:/dev/./urandom --add-opens=java.base/java.lang=ALL-UNNAMED"
WORKDIR $APP_DIR
COPY --from=build $APP_DIR/build/libs/service-authorization-*exec.jar .
VOLUME ["/tmp"]
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS} -jar ${APP_DIR}/service-authorization-*exec.jar"]
