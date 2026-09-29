# ---------- Stage 1: build ----------
FROM maven:3.9-eclipse-temurin-17 AS build

WORKDIR /build

# Cache dependencies separately from source so rebuilds are fast
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
RUN mvn -B clean package -DskipTests

# ---------- Stage 2: runtime ----------
# Tomcat base image because the app is packaged as a WAR with
# spring-boot-starter-tomcat marked as "provided" scope.
FROM tomcat:9-jre17-temurin

# Remove default webapps so only our app is deployed
RUN rm -rf /usr/local/tomcat/webapps/*

# curl is needed by the HEALTHCHECK below. The base image does not ship it.
RUN apt-get update \
    && apt-get install -y --no-install-recommends curl \
    && rm -rf /var/lib/apt/lists/*

# Run Tomcat as a non-root user for container security
RUN groupadd -r appgroup && useradd -r -g appgroup appuser
RUN chown -R appuser:appgroup /usr/local/tomcat

COPY --from=build /build/target/*.war /usr/local/tomcat/webapps/ROOT.war

# Container profile: TLS is handled by the Nginx sidecar or the ALB,
# so the app itself runs plain HTTP on 8080.
# This profile is REQUIRED in a container: the default profile enables
# SSL with a keystore that is deliberately not committed, so the app
# would fail to start without this.
ENV SPRING_PROFILES_ACTIVE=container

# Tomcat tuning: smaller heap suitable for a single container
ENV CATALINA_OPTS="-Xms256m -Xmx512m"

# Actuator health probe used by Docker and Kubernetes
EXPOSE 8080

HEALTHCHECK --interval=30s --timeout=5s --start-period=90s --retries=5 \
    CMD curl -fsS http://localhost:8080/actuator/health || exit 1

USER appuser
CMD ["catalina.sh", "run"]
