package com.ecommerce.product.controller;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.exception.InvalidProductException;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Controller-тесты для ProductController.
 * Тестирует REST API для управления товарами.
 */
@WebMvcTest(ProductController.class)
@DisplayName("ProductController Tests")
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    private ProductResponse createTestProductResponse() {
        return ProductResponse.builder()
                .id(1L)
                .name("Test Product")
                .description("Test Description")
                .price(new BigDecimal("99.99"))
                .stock(100)
                .categoryId(1L)
                .categoryName("Electronics")
                .active(true)
                .build();
    }

    // ========== GET ALL PRODUCTS TESTS ==========

    @Nested
    @DisplayName("GET /api/products")
    class GetAllProductsTests {

        @Test
        @DisplayName("должен вернуть список активных товаров")
        void shouldReturnListOfProducts() throws Exception {
            // Given
            when(productService.getAllProducts())
                    .thenReturn(List.of(createTestProductResponse()));

            // When & Then
            mockMvc.perform(get("/api/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1))
                    .andExpect(jsonPath("$[0].name").value("Test Product"));
        }

        @Test
        @DisplayName("должен вернуть пустой список если товаров нет")
        void shouldReturnEmptyListWhenNoProducts() throws Exception {
            // Given
            when(productService.getAllProducts()).thenReturn(List.of());

            // When & Then
            mockMvc.perform(get("/api/products"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(0));
        }
    }

    // ========== GET PRODUCT BY ID TESTS ==========

    @Nested
    @DisplayName("GET /api/products/{id}")
    class GetProductByIdTests {

        @Test
        @DisplayName("должен вернуть товар по ID")
        void shouldReturnProductById() throws Exception {
            // Given
            when(productService.getProductById(1L))
                    .thenReturn(createTestProductResponse());

            // When & Then
            mockMvc.perform(get("/api/products/{id}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Test Product"));
        }

        @Test
        @DisplayName("должен вернуть 404 если товар не найден")
        void shouldReturn404WhenProductNotFound() throws Exception {
            // Given
            when(productService.getProductById(999L))
                    .thenThrow(new ProductNotFoundException("Product not found with id: 999"));

            // When & Then
            mockMvc.perform(get("/api/products/{id}", 999L))
                    .andExpect(status().isNotFound());
        }
    }

    // ========== GET PRODUCTS BY CATEGORY TESTS ==========

    @Nested
    @DisplayName("GET /api/products/category/{categoryId}")
    class GetProductsByCategoryTests {

        @Test
        @DisplayName("должен вернуть товары по категории")
        void shouldReturnProductsByCategory() throws Exception {
            // Given
            when(productService.getProductsByCategory(1L))
                    .thenReturn(List.of(createTestProductResponse()));

            // When & Then
            mockMvc.perform(get("/api/products/category/{categoryId}", 1L))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }

        @Test
        @DisplayName("должен вернуть 400 если категория не найдена")
        void shouldReturn400WhenCategoryNotFound() throws Exception {
            // Given
            when(productService.getProductsByCategory(999L))
                    .thenThrow(new InvalidProductException("Category not found with id: 999"));

            // When & Then
            mockMvc.perform(get("/api/products/category/{categoryId}", 999L))
                    .andExpect(status().isBadRequest());
        }
    }

    // ========== SEARCH PRODUCTS TESTS ==========

    @Nested
    @DisplayName("GET /api/products/search")
    class SearchProductsTests {

        @Test
        @DisplayName("должен найти товары по названию")
        void shouldSearchProductsByName() throws Exception {
            // Given
            Page<ProductResponse> page = new PageImpl<>(List.of(createTestProductResponse()));
            when(productService.searchProducts(eq("Test"), any(Pageable.class)))
                    .thenReturn(page);

            // When & Then
            mockMvc.perform(get("/api/products/search")
                            .param("name", "Test"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.content.length()").value(1));
        }
    }

    // ========== CREATE PRODUCT TESTS ==========

    @Nested
    @DisplayName("POST /api/products")
    class CreateProductTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("admin должен создать товар")
        void adminShouldCreateProduct() throws Exception {
            // Given
            ProductRequest request = ProductRequest.builder()
                    .name("New Product")
                    .description("Description")
                    .price(new BigDecimal("149.99"))
                    .stock(50)
                    .categoryId(1L)
                    .active(true)
                    .build();

            when(productService.createProduct(any(ProductRequest.class)))
                    .thenReturn(createTestProductResponse());

            // When & Then
            mockMvc.perform(post("/api/products")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.id").value(1));
        }

        @Test
        @WithMockUser(roles = "USER")
        @DisplayName("обычный пользователь не может создать товар")
        void regularUserCannotCreateProduct() throws Exception {
            // Given
            ProductRequest request = ProductRequest.builder()
                    .name("New Product")
                    .price(new BigDecimal("99.99"))
                    .categoryId(1L)
                    .build();

            // When & Then
            mockMvc.perform(post("/api/products")
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }
    }

    // ========== UPDATE PRODUCT TESTS ==========

    @Nested
    @DisplayName("PUT /api/products/{id}")
    class UpdateProductTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("admin должен обновить товар")
        void adminShouldUpdateProduct() throws Exception {
            // Given
            ProductRequest request = ProductRequest.builder()
                    .name("Updated Product")
                    .description("Updated Description")
                    .price(new BigDecimal("199.99"))
                    .stock(75)
                    .categoryId(1L)
                    .active(true)
                    .build();

            ProductResponse updatedResponse = ProductResponse.builder()
                    .id(1L)
                    .name("Updated Product")
                    .price(new BigDecimal("199.99"))
                    .build();

            when(productService.updateProduct(eq(1L), any(ProductRequest.class)))
                    .thenReturn(updatedResponse);

            // When & Then
            mockMvc.perform(put("/api/products/{id}", 1L)
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("Updated Product"));
        }
    }

    // ========== DELETE PRODUCT TESTS ==========

    @Nested
    @DisplayName("DELETE /api/products/{id}")
    class DeleteProductTests {

        @Test
        @WithMockUser(roles = "ADMIN")
        @DisplayName("admin должен удалить товар")
        void adminShouldDeleteProduct() throws Exception {
            // Given
            doNothing().when(productService).deleteProduct(1L);

            // When & Then
            mockMvc.perform(delete("/api/products/{id}", 1L)
                            .with(csrf()))
                    .andExpect(status().isNoContent());

            verify(productService).deleteProduct(1L);
        }
    }

    // ========== GET IN STOCK TESTS ==========

    @Nested
    @DisplayName("GET /api/products/in-stock")
    class GetInStockTests {

        @Test
        @DisplayName("должен вернуть товары в наличии")
        void shouldReturnProductsInStock() throws Exception {
            // Given
            when(productService.getProductsInStock())
                    .thenReturn(List.of(createTestProductResponse()));

            // When & Then
            mockMvc.perform(get("/api/products/in-stock"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }

    // ========== GET TOP RATED TESTS ==========

    @Nested
    @DisplayName("GET /api/products/top-rated")
    class GetTopRatedTests {

        @Test
        @DisplayName("должен вернуть топ товаров по рейтингу")
        void shouldReturnTopRatedProducts() throws Exception {
            // Given
            when(productService.getTopRatedProducts())
                    .thenReturn(List.of(createTestProductResponse()));

            // When & Then
            mockMvc.perform(get("/api/products/top-rated"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.length()").value(1));
        }
    }
}

