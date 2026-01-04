import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    java
    id("org.springframework.boot") version "3.2.0" apply false
    id("io.spring.dependency-management") version "1.1.4"
}

allprojects {
    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    group = "com.ecommerce"
    version = "1.0.0"

    java {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    tasks.withType<JavaCompile> {
        options.encoding = "UTF-8"
        options.compilerArgs.add("-parameters")
    }

    tasks.withType<Test> {
        useJUnitPlatform()
        testLogging {
            events(TestLogEvent.PASSED, TestLogEvent.FAILED, TestLogEvent.SKIPPED)
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
}
subprojects {
    apply(plugin = "org.springframework.boot")
    apply(plugin = "io.spring.dependency-management")

    dependencyManagement {
        imports {
            mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
            mavenBom("org.springframework.cloud:spring-cloud-dependencies:2023.0.0")
        }
    }

    dependencies {
        implementation("org.springframework.boot:spring-boot-starter-web")
        implementation("org.springframework.boot:spring-boot-starter-data-jpa")
        implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
        implementation("org.springframework.boot:spring-boot-starter-security")
        implementation("org.springframework.boot:spring-boot-starter-actuator")
        implementation("org.springframework.boot:spring-boot-starter-data-redis")

        implementation("org.springframework.cloud:spring-cloud-starter-config")

        implementation("io.jsonwebtoken:jjwt-api:0.12.3")
        runtimeOnly("io.jsonwebtoken:jjwt-impl:0.12.3")
        runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.12.3")

        implementation("org.springframework.kafka:spring-kafka")

        runtimeOnly("org.postgresql:postgresql")
        runtimeOnly("org.mongodb:mongodb-driver-sync")

        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")
        testCompileOnly("org.projectlombok:lombok")
        testAnnotationProcessor("org.projectlombok:lombok")

        implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.0.2")

        implementation("io.micrometer:micrometer-registry-prometheus")

        implementation("io.grpc:grpc-netty-shaded:1.59.0")
        implementation("io.grpc:grpc-protobuf:1.59.0")
        implementation("io.grpc:grpc-stub:1.59.0")
        implementation("com.google.protobuf:protobuf-java:3.24.0")

        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testImplementation("org.springframework.kafka:spring-kafka-test")
        testImplementation("org.springframework.security:spring-security-test")
        testImplementation("org.testcontainers:testcontainers:1.19.1")
        testImplementation("org.testcontainers:postgresql:1.19.1")
        testImplementation("org.testcontainers:mongodb:1.19.1")
    }

    tasks.named<BootJar>("bootJar") {
        archiveFileName.set("${project.name}.jar")
    }
}