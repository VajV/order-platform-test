plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    java
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

    // Actuator
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
