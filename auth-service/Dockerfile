FROM eclipse-temurin:21-jre-alpine

WORKDIR /app
COPY build/libs/auth-service*.jar app.jar
RUN addgroup -S spring && adduser -S spring -G spring
USER spring
EXPOSE 8087
ENTRYPOINT ["java", "-jar", "app.jar"]
