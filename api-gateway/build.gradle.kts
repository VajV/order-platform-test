plugins {
    id("org.springframework.boot") version "3.2.1"
    id("io.spring.dependency-management") version "1.1.4"
    id("java")
}

group = "com.ecommerce"
version = "0.0.1-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

dependencies {
    // API Gateway & WebFlux
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")

    // Redis
    implementation("org.springframework.boot:spring-boot-starter-data-redis-reactive")

    // JWT
    implementation("io.jsonwebtoken:jjwt-api:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

    // Swagger (OpenAPI)
    implementation("org.springdoc:springdoc-openapi-starter-webflux-ui:2.3.0")

    // ✅ ДОБАВЛЕНА ЭТА ЗАВИСИМОСТЬ (ДЛЯ /actuator/health)
    implementation("org.springframework.boot:spring-boot-starter-actuator")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
}

dependencyManagement {
    imports {
        mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.0")
    }
}

tasks.test {
    useJUnitPlatform()
}
