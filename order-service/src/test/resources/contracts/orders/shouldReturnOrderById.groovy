package contracts.orders

import org.springframework.cloud.contract.spec.Contract

/**
 * Контракт: GET /api/orders/{id}
 * 
 * Consumer: api-gateway, frontend
 * Producer: order-service
 * 
 * Гарантирует, что order-service возвращает заказ в ожидаемом формате.
 */
Contract.make {
    name "should return order by id"
    description "Returns order details when order exists"
    
    request {
        method GET()
        url "/api/orders/1"
        headers {
            accept(applicationJson())
        }
    }
    
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body([
            id: 1,
            userId: 100,
            status: "NEW",
            items: [[
                id: 1,
                productId: "507f1f77bcf86cd799439011",
                productName: "Test Product",
                quantity: 2,
                unitPrice: 99.99,
                totalPrice: 199.98
            ]],
            totalPrice: 199.98,
            createdAt: $(regex("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}")),
            updatedAt: $(regex("[0-9]{4}-[0-9]{2}-[0-9]{2}T[0-9]{2}:[0-9]{2}:[0-9]{2}"))
        ])
        bodyMatchers {
            jsonPath('$.id', byEquality())
            jsonPath('$.userId', byEquality())
            jsonPath('$.status', byEquality())
            jsonPath('$.totalPrice', byEquality())
            jsonPath('$.items', byType { minOccurrence(1) })
        }
    }
}

