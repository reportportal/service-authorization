FROM openjdk:11-jre-slim
LABEL version=5.3.1 description="Unified Authorization Trap for all ReportPortal's Services" maintainer="Andrei Varabyeu <andrei_varabyeu@epam.com>"
RUN apt-get update -qq && apt-get install -qq -y wget && \
	wget -q https://dl.bintray.com/epam/reportportal/com/epam/reportportal/service-authorization/5.3.1/service-authorization-5.3.1-exec.jar
ENV JAVA_OPTS="-Xmx512m -XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=70 -Djava.security.egd=file:/dev/./urandom"
VOLUME ["/tmp"]
EXPOSE 8080
COPY entrypoint.sh ./entrypoint.sh
ENTRYPOINT ./entrypoint.sh
