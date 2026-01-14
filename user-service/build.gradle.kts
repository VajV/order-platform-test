plugins {
    java
    id("org.springframework.boot") version "3.2.1"
    id("io.spring.dependency-management") version "1.1.4"
    id("com.github.davidmc24.gradle.plugin.avro") version "1.9.1"  // ← ДОБАВЬ
}

dependencies {
    // Spring Boot Web & Data
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")

    // PostgreSQL Driver
    implementation("org.postgresql:postgresql:42.7.1")

    // Flyway DB Migration (ИСПРАВЛЕНО)
    implementation("org.flywaydb:flyway-core:10.4.1")
    runtimeOnly("org.flywaydb:flyway-database-postgresql:10.4.1")

    // Spring Security & JWT
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // Kafka & Avro
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.apache.kafka:kafka-clients:3.6.1")
    implementation("io.confluent:kafka-avro-serializer:7.5.0") {
        exclude(group = "org.apache.kafka", module = "kafka-clients")
    }
    implementation("org.apache.avro:avro:1.11.3")

    // Lombok
    compileOnly("org.projectlombok:lombok")
    annotationProcessor("org.projectlombok:lombok")

    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")

    // OpenAPI/Swagger
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.2.0")

    // Testing
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:postgresql:1.19.3")
    testImplementation("org.testcontainers:kafka:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    testRuntimeOnly("com.h2database:h2:2.2.224")
}

avro {
    fieldVisibility.set("PRIVATE")
    outputCharacterEncoding.set("UTF-8")
}
