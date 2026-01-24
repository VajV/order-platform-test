package com.ecommerce.product.contract;

import com.ecommerce.product.controller.ProductController;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.service.ProductService;
import io.restassured.module.mockmvc.RestAssuredMockMvc;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.ecommerce.product.config.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
@Import(TestSecurityConfig.class)
@ActiveProfiles("test")
public abstract class BaseContractTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ProductService productService;

    @MockBean
    private JwtDecoder jwtDecoder;

    @BeforeEach
    void setup() {
        RestAssuredMockMvc.mockMvc(mockMvc);
        setupMocks();
    }

    private void setupMocks() {
        ProductResponse product = createSampleProduct();
        
        when(productService.getProductById(anyString())).thenReturn(product);
        when(productService.getAllProducts()).thenReturn(List.of(product));
        when(productService.getProductsByCategory(anyString(), any())).thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(product)));
        when(productService.getProductsInStock()).thenReturn(List.of(product));
        when(productService.searchProducts(any(), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(product)));
    }

    protected ProductResponse createSampleProduct() {
        return ProductResponse.builder()
                .id("p1")
                .name("Test Product")
                .description("High-quality test product for contract testing")
                .price(new BigDecimal("99.99"))
                .category("Electronics")
                .isPublished(true)
                .isDeleted(false)
                .build();
    }
}

