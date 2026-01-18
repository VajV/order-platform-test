package contracts.messaging

import org.springframework.cloud.contract.spec.Contract

/**
 * Контракт: Kafka Event - order.status-changed
 * 
 * Producer: order-service
 * Consumers: notification-service, analytics-service
 * 
 * Гарантирует формат события при изменении статуса заказа.
 */
Contract.make {
    name "should publish order status changed event"
    description "When order status changes, publishes event to order.status-changed topic"
    
    label "order_status_changed"
    
    input {
        triggeredBy("publishOrderStatusChangedEvent()")
    }
    
    outputMessage {
        sentTo "order.status-changed"
        headers {
            header("kafka_messageKey", $(regex("[0-9]+")))
            header("contentType", "application/json")
        }
        body([
            orderId: $(anyNumber()),
            previousStatus: $(regex("NEW|RESERVED|PAID|SHIPPED|COMPLETED|CANCELLED")),
            newStatus: $(regex("NEW|RESERVED|PAID|SHIPPED|COMPLETED|CANCELLED")),
            reason: $(anyNonBlankString()),
            timestamp: $(regex("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}.*"))
        ])
        bodyMatchers {
            jsonPath('$.orderId', byRegex("[0-9]+"))
            jsonPath('$.previousStatus', byRegex("NEW|RESERVED|PAID|SHIPPED|COMPLETED|CANCELLED"))
            jsonPath('$.newStatus', byRegex("NEW|RESERVED|PAID|SHIPPED|COMPLETED|CANCELLED"))
        }
    }
}

