plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    java
}

description = "product-service"

tasks.bootRun {
    args = listOf("--spring.profiles.active=dev")
}
