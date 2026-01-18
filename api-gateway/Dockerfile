FROM eclipse-temurin:21-jre-alpine

WORKDIR /app

# Copy only the bootJar (not the -plain.jar)
COPY build/libs/api-gateway.jar app.jar

# Security: run as non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
