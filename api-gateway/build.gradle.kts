plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    java
}

description = "API Gateway"

dependencies {
    implementation("org.springframework.cloud:spring-cloud-starter-gateway")
}

tasks.bootRun {
    args = listOf("--spring.profiles.active=dev")
}
