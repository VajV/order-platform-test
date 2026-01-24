package com.ecommerce.inventory;

import com.ecommerce.inventory.config.TestSecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.ANY)
@EmbeddedKafka(partitions = 1, topics = {"order.created", "order.payment-confirmed", "order.cancelled"})
class InventoryServiceApplicationTests {

    @Test
    void contextLoads() {
        // Context loads successfully with H2 and embedded Kafka
    }
}