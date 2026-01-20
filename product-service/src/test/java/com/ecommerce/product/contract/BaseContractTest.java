package com.ecommerce.product.contract;

import com.ecommerce.product.controller.ProductController;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.service.ProductService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;

/**
 * Базовый класс для Contract Tests product-service.
 * 
 * Контракты гарантируют стабильность API для consumers:
 * - order-service (получение информации о товарах)
 * - api-gateway (проксирование запросов)
 * - frontend (отображение каталога)
 */
@WebMvcTest(ProductController.class)
@ExtendWith(MockitoExtension.class)
public abstract class BaseContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @BeforeEach
    void setup() {
        RestAssuredMockMvc.mockMvc(mockMvc);
        setupMocks();
    }

    private void setupMocks() {
        ProductResponse product = createSampleProduct();
        
        when(productService.getProductById(anyLong())).thenReturn(product);
        when(productService.getAllProducts()).thenReturn(List.of(product));
        when(productService.getProductsByCategory(anyLong())).thenReturn(List.of(product));
        when(productService.getProductsInStock()).thenReturn(List.of(product));
        when(productService.searchProducts(any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(product)));
    }

    protected ProductResponse createSampleProduct() {
        return ProductResponse.builder()
                .id(1L)
                .name("Test Product")
                .description("High-quality test product for contract testing")
                .price(new BigDecimal("99.99"))
                .stock(50)
                .categoryId(1L)
                .categoryName("Electronics")
                .active(true)
                .rating(4.5)
                .build();
    }
}

