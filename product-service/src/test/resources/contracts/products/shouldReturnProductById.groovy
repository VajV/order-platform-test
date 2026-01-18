package contracts.products

import org.springframework.cloud.contract.spec.Contract

/**
 * Контракт: GET /api/products/{id}
 * 
 * Consumer: order-service, frontend
 * Producer: product-service
 */
Contract.make {
    name "should return product by id"
    description "Returns product details when product exists"
    
    request {
        method GET()
        url "/api/products/1"
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
            name: "Test Product",
            description: $(anyNonBlankString()),
            price: 99.99,
            stock: $(anyPositiveInt()),
            categoryId: 1,
            categoryName: "Electronics",
            active: true,
            rating: $(anyNumber())
        ])
        bodyMatchers {
            jsonPath('$.id', byEquality())
            jsonPath('$.name', byEquality())
            jsonPath('$.price', byEquality())
            jsonPath('$.active', byEquality())
        }
    }
}

