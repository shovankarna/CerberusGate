# Dockerfile
FROM openjdk:17-jdk-slim

COPY target/*.jar /app/gateway.jar

EXPOSE 8069

ENTRYPOINT ["java", "-jar", "/app/gateway.jar"]