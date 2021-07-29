FROM alpine:latest
LABEL version=5.5.0 description="Unified Authorization Trap for all ReportPortal's Services" maintainer="Andrei Varabyeu <andrei_varabyeu@epam.com>, Hleb Kanonik <hleb_kanonik@epam.com>"
ARG GH_TOKEN
RUN apk -U -q upgrade && apk --no-cache -q add openjdk11 ca-certificates && \
	echo 'exec java ${JAVA_OPTS} -jar service-authorization-5.5.0-exec.jar' > /start.sh && chmod +x /start.sh && \
	wget --header="Authorization: Bearer ${GH_TOKEN}"  -q https://maven.pkg.github.com/reportportal/service-authorization/com/epam/reportportal/service-authorization/5.5.0/service-authorization-5.5.0-exec.jar
ENV JAVA_OPTS="-Xmx512m -XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=70 -Djava.security.egd=file:/dev/./urandom"
VOLUME ["/tmp"]
EXPOSE 8080
ENTRYPOINT ./start.sh
