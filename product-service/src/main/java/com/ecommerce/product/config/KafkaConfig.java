package com.ecommerce.product.config;

import com.ecommerce.product.dto.ProductEvent;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Конфигурация Apache Kafka
 * Настраивает producer для отправки событий
 */
@Configuration
@EnableKafka
public class KafkaConfig {

    @Value("${spring.kafka.bootstrap-servers:localhost:9092}")
    private String bootstrapServers;

    /**
     * Создает KafkaTemplate для отправки сообщений
     */
    @Bean
    public KafkaTemplate<String, ProductEvent> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    /**
     * Настраивает Producer Factory
     */
    @Bean
    public ProducerFactory<String, ProductEvent> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();

        // Адрес Kafka брокера
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);

        // Сериализаторы для key и value
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);

        // Подтверждение от всех реплик (надежность)
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");

        // Количество повторов при ошибке
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);

        // Включить идемпотентность (предотвращение дубликатов)
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);

        // Batch настройки для производительности
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 10);

        // Compression для уменьшения размера
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");

        // Buffer memory
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);

        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Настройки JsonSerializer (опционально)
     */
    @Bean
    public JsonSerializer<ProductEvent> jsonSerializer() {
        JsonSerializer<ProductEvent> serializer = new JsonSerializer<>();
        serializer.setAddTypeInfo(false); // Не добавлять информацию о типе в JSON
        return serializer;
    }
}
