FROM openjdk:11-jre-slim
LABEL version=5.3.3 description="Unified Authorization Trap for all ReportPortal's Services" maintainer="Andrei Varabyeu <andrei_varabyeu@epam.com>"
RUN apt-get update -qq && apt-get install -qq -y wget && \
	echo 'exec java ${JAVA_OPTS} -jar service-authorization-5.3.3-exec.jar' > /start.sh && chmod +x /start.sh && \
	wget -q https://dl.bintray.com/epam/reportportal/com/epam/reportportal/service-authorization/5.3.3/service-authorization-5.3.3-exec.jar
ENV JAVA_OPTS="-Xmx512m -XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=70 -Djava.security.egd=file:/dev/./urandom"
VOLUME ["/tmp"]
EXPOSE 8080
ENTRYPOINT ./start.sh
