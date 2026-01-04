plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    java
}

description = "order-service"

tasks.bootRun {
    args = listOf("--spring.profiles.active=dev")
}
