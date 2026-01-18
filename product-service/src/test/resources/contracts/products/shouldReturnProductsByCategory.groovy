package contracts.products

import org.springframework.cloud.contract.spec.Contract

/**
 * Контракт: GET /api/products/category/{categoryId}
 * 
 * Consumer: frontend, catalog service
 * Producer: product-service
 */
Contract.make {
    name "should return products by category"
    description "Returns list of products in a specific category"
    
    request {
        method GET()
        url "/api/products/category/1"
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
            categoryId: 1,
            active: true
        ]])
        bodyMatchers {
            jsonPath('$', byType { minOccurrence(0) })
            jsonPath('$[*].categoryId', byEquality())
        }
    }
}

