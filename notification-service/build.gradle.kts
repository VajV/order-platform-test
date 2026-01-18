dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-mongodb")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-mail")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.cloud:spring-cloud-starter-vault-config")

    // Kafka
    implementation("org.springframework.kafka:spring-kafka")

    // Kafka Avro + Schema Registry
    implementation("org.apache.kafka:kafka-clients:3.6.1")
    implementation("io.confluent:kafka-avro-serializer:7.5.0")
    implementation("io.confluent:kafka-schema-registry-client:7.5.0")

    // Redis Client
    implementation("io.lettuce:lettuce-core")
    implementation("redis.clients:jedis:5.1.0")

    // JSON Processing
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")

    // Jakarta Mail (вместо javax.mail)
    implementation("com.sun.mail:jakarta.mail:2.0.1")

    // Logging
    implementation("net.logstash.logback:logstash-logback-encoder:7.4")

    // Prometheus
    runtimeOnly("io.micrometer:micrometer-registry-prometheus")

    // Testing
    testImplementation("org.springframework.kafka:spring-kafka-test")
    testImplementation("org.testcontainers:testcontainers:1.19.3")
    testImplementation("org.testcontainers:mongodb:1.19.3")
    testImplementation("org.testcontainers:kafka:1.19.3")
    testImplementation("org.testcontainers:junit-jupiter:1.19.3")
    testImplementation("com.icegreen:greenmail-junit5:2.0.1")
    testImplementation("org.awaitility:awaitility:4.2.0")
}

tasks.withType<Test> {
    systemProperty("spring.profiles.active", "test")
}
