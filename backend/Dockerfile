# ---------- Stage 1: build the jar ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /src
# Copy pom first so dependency download is cached between builds
COPY pom.xml .
RUN mvn -q -B dependency:go-offline
COPY src ./src
RUN mvn -q -B -DskipTests package

# ---------- Stage 2: small runtime image ----------
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
RUN addgroup -S app && adduser -S app -G app
COPY --from=build /src/target/app.jar app.jar
USER app

# Memory-friendly JVM for Render free tier (512 MB RAM total):
#  -Xmx256m              heap cap; leaves room for metaspace, threads and the OS
#  -XX:+UseSerialGC      simplest GC, lowest memory overhead (perfect for 1 user)
#  -XX:+UseContainerSupport  read the container's memory limit (default on, stated for clarity)
#  -XX:MaxMetaspaceSize=128m, -Xss512k  keep class metadata and thread stacks small
#  -XX:TieredStopAtLevel=1   faster startup (helps cold starts) at a small peak-speed cost
ENV JAVA_OPTS="-Xmx256m -Xss512k -XX:MaxMetaspaceSize=128m -XX:+UseSerialGC -XX:+UseContainerSupport -XX:TieredStopAtLevel=1 -Djava.security.egd=file:/dev/./urandom"
ENV SERVER_TOMCAT_THREADS_MAX=20

EXPOSE 8080
# Render sets $PORT; application.yml reads it (server.port: ${PORT:8080})
ENTRYPOINT ["sh", "-c", "exec java $JAVA_OPTS -jar app.jar"]
