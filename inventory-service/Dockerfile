FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

COPY build/libs/inventory-service*.jar app.jar

RUN addgroup -S spring && adduser -S spring -G spring
USER spring

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]
