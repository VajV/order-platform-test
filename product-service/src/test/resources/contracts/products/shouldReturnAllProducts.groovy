package contracts.products

import org.springframework.cloud.contract.spec.Contract

/**
 * Контракт: GET /api/products
 * 
 * Consumer: frontend, api-gateway
 * Producer: product-service
 */
Contract.make {
    name "should return all products"
    description "Returns list of all active products"
    
    request {
        method GET()
        url "/api/products"
        headers {
            accept(applicationJson())
        }
    }
    
    response {
        status OK()
        headers {
            contentType(applicationJson())
        }
        body([[
            id: $(anyNumber()),
            name: $(anyNonBlankString()),
            price: $(anyNumber()),
            stock: $(anyPositiveInt()),
            categoryId: $(anyNumber()),
            active: true
        ]])
        bodyMatchers {
            jsonPath('$', byType { minOccurrence(0) })
            jsonPath('$[*].active', byEquality())
        }
    }
}

