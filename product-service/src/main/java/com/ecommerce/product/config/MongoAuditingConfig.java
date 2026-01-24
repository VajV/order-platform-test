package com.ecommerce.product.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@Configuration
@Profile("!test")
@EnableMongoAuditing
public class MongoAuditingConfig {
}
