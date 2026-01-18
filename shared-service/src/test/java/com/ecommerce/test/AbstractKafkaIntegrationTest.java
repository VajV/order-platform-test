package com.ecommerce.test;

import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

/**
 * Базовый класс для интеграционных тестов с Kafka.
 * Использует Testcontainers для запуска реального Kafka брокера.
 * 
 * Использование:
 * <pre>
 * {@code
 * @SpringBootTest
 * class MyKafkaIntegrationTest extends AbstractKafkaIntegrationTest {
 *     @Autowired
 *     private KafkaTemplate<String, Object> kafkaTemplate;
 *     
 *     @Test
 *     void testKafkaProducer() {
 *         kafkaTemplate.send("topic", "message");
 *     }
 * }
 * }
 * </pre>
 */
@Testcontainers
public abstract class AbstractKafkaIntegrationTest {

    @Container
    protected static KafkaContainer kafka = new KafkaContainer(
            DockerImageName.parse("confluentinc/cp-kafka:7.5.0")
    ).withReuse(true);

    @DynamicPropertySource
    static void kafkaProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
        registry.add("spring.kafka.consumer.auto-offset-reset", () -> "earliest");
        registry.add("spring.kafka.consumer.group-id", () -> "test-group");
        
        // Отключаем Vault
        registry.add("spring.cloud.vault.enabled", () -> "false");
        registry.add("spring.config.import", () -> "");
    }
}

