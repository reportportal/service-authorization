FROM amazoncorretto:11.0.17
LABEL version=5.7.4 description="Unified Authorization Trap for all ReportPortal's Services" maintainer="Andrei Varabyeu <andrei_varabyeu@epam.com>, Hleb Kanonik <hleb_kanonik@epam.com>"
ARG GH_TOKEN
RUN echo 'exec java ${JAVA_OPTS} -jar service-authorization-5.7.4-exec.jar' > /start.sh && chmod +x /start.sh && \
	wget --header="Authorization: Bearer ${GH_TOKEN}"  -q https://maven.pkg.github.com/reportportal/service-authorization/com/epam/reportportal/service-authorization/5.7.4/service-authorization-5.7.4-exec.jar
ENV JAVA_OPTS="-Xmx512m -XX:+UseG1GC -XX:InitiatingHeapOccupancyPercent=70 -Djava.security.egd=file:/dev/./urandom"
VOLUME ["/tmp"]
EXPOSE 8080
ENTRYPOINT ./start.sh
