// InventoryServiceApplication.java
package com.ecommerce.inventory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Inventory Service entry point.
 * Manages product stock and reservations with event-driven architecture.
 *
 * Ports:
 * - Service: 8084
 * - Health: 8084/actuator/health
 * - Swagger: 8084/swagger-ui.html
 */
@SpringBootApplication
@EnableTransactionManagement
@EnableKafka
@EnableAsync
public class InventoryServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(InventoryServiceApplication.class, args);
    }
}
