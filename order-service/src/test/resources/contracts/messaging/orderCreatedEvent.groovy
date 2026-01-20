package contracts.messaging

import org.springframework.cloud.contract.spec.Contract

/**
 * Контракт: Kafka Event - order.created
 * 
 * Producer: order-service
 * Consumers: inventory-service, notification-service
 * 
 * Гарантирует формат события при создании заказа.
 */
Contract.make {
    name "should publish order created event"
    description "When order is created, publishes event to order.created topic"
    
    label "order_created"
    
    input {
        triggeredBy("publishOrderCreatedEvent()")
    }
    
    outputMessage {
        sentTo "order.created"
        headers {
            header("kafka_messageKey", $(regex("[0-9]+")))
            header("contentType", "application/json")
        }
        body([
            orderId: $(anyNumber()),
            userId: $(anyNumber()),
            items: [[
                productId: $(anyNonBlankString()),
                productName: $(anyNonBlankString()),
                quantity: $(anyPositiveInt()),
                unitPrice: $(anyNumber())
            ]],
            totalPrice: $(anyNumber()),
            timestamp: $(regex("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.*"))
        ])
        bodyMatchers {
            jsonPath('$.orderId', byRegex("[0-9]+"))
            jsonPath('$.userId', byRegex("[0-9]+"))
            jsonPath('$.items', byType { minOccurrence(1) })
        }
    }
}

