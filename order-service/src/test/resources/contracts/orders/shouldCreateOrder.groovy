package contracts.orders

import org.springframework.cloud.contract.spec.Contract

/**
 * Контракт: POST /api/orders
 * 
 * Consumer: api-gateway, frontend
 * Producer: order-service
 * 
 * Гарантирует формат создания заказа.
 */
Contract.make {
    name "should create new order"
    description "Creates a new order and returns it with generated ID"
    
    request {
        method POST()
        url "/api/orders"
        headers {
            contentType(applicationJson())
            accept(applicationJson())
        }
        body([
            userId: 100,
            items: [[
                productId: "507f1f77bcf86cd799439011",
                productName: "Test Product",
                quantity: 2,
                unitPrice: 99.99
            ]]
        ])
    }
    
    response {
        status CREATED()
        headers {
            contentType(applicationJson())
        }
        body([
            id: $(anyNumber()),
            userId: 100,
            status: "NEW",
            items: [[
                id: $(anyNumber()),
                productId: "507f1f77bcf86cd799439011",
                productName: "Test Product",
                quantity: 2,
                unitPrice: 99.99,
                totalPrice: 199.98
            ]],
            totalPrice: 199.98
        ])
        bodyMatchers {
            jsonPath('$.id', byRegex("[0-9]+"))
            jsonPath('$.status', byEquality())
            jsonPath('$.userId', byEquality())
        }
    }
}

