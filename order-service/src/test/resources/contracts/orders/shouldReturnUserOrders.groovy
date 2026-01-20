package contracts.orders

import org.springframework.cloud.contract.spec.Contract

/**
 * Контракт: GET /api/v1/orders?userId={userId}
 * 
 * Consumer: api-gateway, user dashboard
 * Producer: order-service
 * 
 * Гарантирует формат списка заказов пользователя с пагинацией.
 */
Contract.make {
    name "should return user orders with pagination"
    description "Returns paginated list of orders for a specific user"
    
    request {
        method GET()
        url("/api/v1/orders") {
            queryParameters {
                parameter "userId": 100
                parameter "page": 0
                parameter "size": 10
            }
        }
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
            content: [[
                id: $(anyNumber()),
                userId: 100,
                status: $(regex("NEW|RESERVED|PAID|SHIPPED|COMPLETED|CANCELLED")),
                totalPrice: $(anyNumber())
            ]],
            pageable: [
                pageNumber: 0,
                pageSize: 10
            ],
            totalElements: $(anyNumber()),
            totalPages: $(anyNumber()),
            last: $(anyBoolean()),
            first: $(anyBoolean())
        ])
        bodyMatchers {
            jsonPath('$.content', byType { minOccurrence(0) })
            jsonPath('$.content[*].userId', byEquality())
        }
    }
}

