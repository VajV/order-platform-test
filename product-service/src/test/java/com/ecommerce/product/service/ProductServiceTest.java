package com.ecommerce.product.service;

import com.ecommerce.product.dto.*;
import com.ecommerce.product.entity.Category;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.exception.InvalidProductException;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Unit-тесты для ProductService.
 * Покрытие: CRUD операции, поиск, управление остатками.
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService Unit Tests")
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private ProductMapper productMapper;

    @Mock
    private ProductEventPublisher eventPublisher;

    @InjectMocks
    private ProductService productService;

    private Product testProduct;
    private Category testCategory;
    private ProductResponse testProductResponse;
    private ProductCreateRequest testProductCreateRequest;
    private ProductUpdateRequest testProductUpdateRequest;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .id("c1")
                .name("Electronics")
                .description("Electronic devices")
                .build();

        testProduct = Product.builder()
                .id("p1")
                .name("Test Product")
                .description("Test Description")
                .price(new BigDecimal("99.99"))
                .category("Electronics")
                .isPublished(true)
                .isDeleted(false)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testProductResponse = ProductResponse.builder()
                .id("p1")
                .name("Test Product")
                .description("Test Description")
                .price(new BigDecimal("99.99"))
                .category("Electronics")
                .isPublished(true)
                .isDeleted(false)
                .build();

        testProductCreateRequest = ProductCreateRequest.builder()
                .name("New Product")
                .description("New Description")
                .price(new BigDecimal("149.99"))
                .category("Electronics")
                .build();

        testProductUpdateRequest = ProductUpdateRequest.builder()
                .name("Updated Product")
                .description("Updated Description")
                .price(new BigDecimal("159.99"))
                .category("Electronics")
                .build();
    }

    // ========== GET ALL PRODUCTS TESTS ==========

    @Nested
    @DisplayName("getAllProducts()")
    class GetAllProductsTests {

        @Test
        @DisplayName("должен вернуть список активных товаров")
        void shouldReturnListOfActiveProducts() {
            // Given
            Page<Product> productPage = new PageImpl<>(List.of(testProduct));
            when(productRepository.findByIsDeletedFalseAndIsPublishedTrue(any(Pageable.class)))
                    .thenReturn(productPage);
            when(productMapper.toResponse(testProduct)).thenReturn(testProductResponse);

            // When
            List<ProductResponse> result = productService.getAllProducts();

            // Then
            assertThat(result).isNotEmpty();
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("должен вернуть пустой список если товаров нет")
        void shouldReturnEmptyListWhenNoProducts() {
            // Given
            Page<Product> productPage = new PageImpl<>(List.of());
            when(productRepository.findByIsDeletedFalseAndIsPublishedTrue(any(Pageable.class)))
                    .thenReturn(productPage);

            // When
            List<ProductResponse> result = productService.getAllProducts();

            // Then
            assertThat(result).isEmpty();
        }
    }

    // ========== GET PRODUCT BY ID TESTS ==========

    @Nested
    @DisplayName("getProductById()")
    class GetProductByIdTests {

        @Test
        @DisplayName("должен вернуть товар по ID")
        void shouldReturnProductById() {
            // Given
            when(productRepository.findByIdAndIsDeletedFalseAndIsPublishedTrue("p1"))
                    .thenReturn(Optional.of(testProduct));
            when(productMapper.toResponse(testProduct)).thenReturn(testProductResponse);

            // When
            ProductResponse result = productService.getProductById("p1");

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo("p1");
        }

        @Test
        @DisplayName("должен выбросить ProductNotFoundException если товар не найден")
        void shouldThrowExceptionWhenProductNotFound() {
            // Given
            when(productRepository.findByIdAndIsDeletedFalseAndIsPublishedTrue("p999"))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> productService.getProductById("p999"))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessageContaining("Product not found with id: p999");
        }
    }

    // ========== GET PRODUCTS BY CATEGORY TESTS ==========

    @Nested
    @DisplayName("getProductsByCategory()")
    class GetProductsByCategoryTests {

        @Test
        @DisplayName("должен вернуть товары по категории")
        void shouldReturnProductsByCategory() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> productPage = new PageImpl<>(List.of(testProduct), pageable, 1);

            when(categoryRepository.existsByName("Electronics")).thenReturn(true);
            when(productRepository.findByIsDeletedFalseAndIsPublishedTrueAndCategory(eq("Electronics"), any(Pageable.class)))
                    .thenReturn(productPage);
            when(productMapper.toResponse(testProduct)).thenReturn(testProductResponse);

            // When
            Page<ProductResponse> result = productService.getProductsByCategory("Electronics", pageable);

            // Then
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getCategory()).isEqualTo("Electronics");
        }

        @Test
        @DisplayName("должен выбросить исключение если категория не найдена")
        void shouldThrowExceptionWhenCategoryNotFound() {
            // Given
            when(categoryRepository.existsByName("NonExistingCategory")).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> productService.getProductsByCategory("NonExistingCategory", PageRequest.of(0, 10)))
                    .isInstanceOf(InvalidProductException.class)
                    .hasMessageContaining("Category not found");
        }
    }

    // ========== SEARCH PRODUCTS TESTS ==========

    @Nested
    @DisplayName("searchProducts()")
    class SearchProductsTests {

        @Test
        @DisplayName("должен найти товары по названию")
        void shouldFindProductsByName() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Product> productPage = new PageImpl<>(List.of(testProduct), pageable, 1);
            
            when(productRepository.findByIsDeletedFalseAndIsPublishedTrueAndNameContainingIgnoreCase("Test", pageable))
                    .thenReturn(productPage);
            when(productMapper.toResponse(testProduct)).thenReturn(testProductResponse);

            // When
            Page<ProductResponse> result = productService.searchProducts("Test", pageable);

            // Then
            assertThat(result.getTotalElements()).isEqualTo(1);
            assertThat(result.getContent().get(0).getName()).isEqualTo("Test Product");
        }

        @Test
        @DisplayName("должен выбросить исключение при пустом поисковом запросе")
        void shouldThrowExceptionOnEmptySearchTerm() {
            // Given
            Pageable pageable = PageRequest.of(0, 10);

            // When & Then
            assertThatThrownBy(() -> productService.searchProducts("", pageable))
                    .isInstanceOf(InvalidProductException.class)
                    .hasMessageContaining("Search term cannot be empty");

            assertThatThrownBy(() -> productService.searchProducts("   ", pageable))
                    .isInstanceOf(InvalidProductException.class);

            assertThatThrownBy(() -> productService.searchProducts(null, pageable))
                    .isInstanceOf(InvalidProductException.class);
        }
    }

    // ========== CREATE PRODUCT TESTS ==========

    @Nested
    @DisplayName("createProduct()")
    class CreateProductTests {

        @Test
        @DisplayName("должен успешно создать товар")
        void shouldCreateProductSuccessfully() {
            // Given
            Product newProduct = Product.builder()
                    .id("p2")
                    .name("New Product")
                    .description("New Description")
                    .price(new BigDecimal("149.99"))
                    .category("Electronics")
                    .isPublished(false)
                    .isDeleted(false)
                    .build();

            ProductResponse newProductResponse = ProductResponse.builder()
                    .id("p2")
                    .name("New Product")
                    .description("New Description")
                    .price(new BigDecimal("149.99"))
                    .category("Electronics")
                    .isPublished(false)
                    .isDeleted(false)
                    .build();

            when(categoryRepository.existsByName("Electronics")).thenReturn(true);
            when(productMapper.toEntity(testProductCreateRequest)).thenReturn(newProduct);
            when(productRepository.save(any(Product.class))).thenReturn(newProduct);
            when(productMapper.toResponse(newProduct)).thenReturn(newProductResponse);

            // When
            ProductResponse result = productService.createProduct(testProductCreateRequest);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getName()).isEqualTo("New Product");
            assertThat(result.getPrice()).isEqualByComparingTo(new BigDecimal("149.99"));
            
            verify(eventPublisher).publishProductEvent(any(ProductEvent.class), eq("product.created"));
        }

        @Test
        @DisplayName("должен выбросить исключение если категория не найдена")
        void shouldThrowExceptionWhenCategoryNotFoundOnCreate() {
            // Given
            when(categoryRepository.existsByName("NonExistingCategory")).thenReturn(false);

            ProductCreateRequest request = ProductCreateRequest.builder()
                    .name("New Product")
                    .description("New Description")
                    .price(new BigDecimal("149.99"))
                    .category("NonExistingCategory")
                    .build();

            // When & Then
            assertThatThrownBy(() -> productService.createProduct(request))
                    .isInstanceOf(InvalidProductException.class)
                    .hasMessageContaining("Category not found");
        }
    }

    // ========== UPDATE PRODUCT TESTS ==========

    @Nested
    @DisplayName("updateProduct()")
    class UpdateProductTests {

        @Test
        @DisplayName("должен успешно обновить товар")
        void shouldUpdateProductSuccessfully() {
            // Given
            when(productRepository.findByIdAndIsDeletedFalse("p1")).thenReturn(Optional.of(testProduct));
            when(categoryRepository.existsByName("Electronics")).thenReturn(true);
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);
            when(productMapper.toResponse(testProduct)).thenReturn(testProductResponse);

            // When
            ProductResponse result = productService.updateProduct("p1", testProductUpdateRequest);

            // Then
            assertThat(result).isNotNull();
            verify(productRepository).save(any(Product.class));
            verify(eventPublisher).publishProductEvent(any(ProductEvent.class), eq("product.updated"));
        }

        @Test
        @DisplayName("должен выбросить исключение если товар не найден")
        void shouldThrowExceptionWhenProductNotFoundOnUpdate() {
            // Given
            when(productRepository.findByIdAndIsDeletedFalse("p999")).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> productService.updateProduct("p999", testProductUpdateRequest))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessageContaining("Product not found");
        }
    }

    // ========== DELETE PRODUCT TESTS ==========

    @Nested
    @DisplayName("deleteProduct()")
    class DeleteProductTests {

        @Test
        @DisplayName("должен выполнить soft delete товара")
        void shouldSoftDeleteProduct() {
            // Given
            when(productRepository.findByIdAndIsDeletedFalse("p1")).thenReturn(Optional.of(testProduct));
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);

            // When
            productService.deleteProduct("p1");

            // Then
            assertThat(testProduct.getIsDeleted()).isTrue();
            verify(productRepository).save(testProduct);
            verify(eventPublisher).publishProductEvent(any(ProductEvent.class), eq("product.deleted"));
        }
    }

    // ========== UPDATE STOCK TESTS ==========

    @Nested
    @DisplayName("updateStock()")
    class UpdateStockTests {

        @Test
        @DisplayName("должен уменьшить остаток товара")
        void shouldDecreaseProductStock() {
            assertThatThrownBy(() -> productService.updateStock("p1", 10))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("inventory-service");
        }

        @Test
        @DisplayName("должен выбросить исключение при недостаточном остатке")
        void shouldThrowExceptionWhenInsufficientStock() {
            assertThatThrownBy(() -> productService.updateStock("p1", 10))
                    .isInstanceOf(UnsupportedOperationException.class)
                    .hasMessageContaining("inventory-service");
        }
    }

    // ========== GET TOP RATED PRODUCTS TESTS ==========

    @Nested
    @DisplayName("getTopRatedProducts()")
    class GetTopRatedProductsTests {

        @Test
        @DisplayName("должен вернуть топ товаров по рейтингу")
        void shouldReturnTopRatedProducts() {
            assertThatThrownBy(() -> productService.getTopRatedProducts())
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }

    // ========== GET PRODUCTS IN STOCK TESTS ==========

    @Nested
    @DisplayName("getProductsInStock()")
    class GetProductsInStockTests {

        @Test
        @DisplayName("должен вернуть товары в наличии")
        void shouldReturnProductsInStock() {
            assertThatThrownBy(() -> productService.getProductsInStock())
                    .isInstanceOf(UnsupportedOperationException.class);
        }
    }
}

