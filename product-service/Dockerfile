FROM eclipse-temurin:25-jre-alpine

WORKDIR /app

# Copy only the bootJar (not the -plain.jar)
COPY build/libs/product-service.jar app.jar

# Security: run as non-root user
RUN addgroup -S spring && adduser -S spring -G spring
USER spring

EXPOSE 8084

ENTRYPOINT ["java", "-jar", "app.jar"]
