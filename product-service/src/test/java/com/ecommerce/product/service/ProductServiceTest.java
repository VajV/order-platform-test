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
import static org.mockito.ArgumentMatchers.anyLong;
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
    private ProductRequest testProductRequest;

    @BeforeEach
    void setUp() {
        testCategory = Category.builder()
                .id(1L)
                .name("Electronics")
                .description("Electronic devices")
                .build();

        testProduct = Product.builder()
                .id(1L)
                .name("Test Product")
                .description("Test Description")
                .price(new BigDecimal("99.99"))
                .stock(100)
                .category(testCategory)
                .active(true)
                .rating(4.5)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        testProductResponse = ProductResponse.builder()
                .id(1L)
                .name("Test Product")
                .description("Test Description")
                .price(new BigDecimal("99.99"))
                .stock(100)
                .categoryId(1L)
                .categoryName("Electronics")
                .active(true)
                .build();

        testProductRequest = ProductRequest.builder()
                .name("New Product")
                .description("New Description")
                .price(new BigDecimal("149.99"))
                .stock(50)
                .categoryId(1L)
                .active(true)
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
            when(productRepository.findByActiveTrueOrderByCreatedAtDesc())
                    .thenReturn(List.of(testProduct));
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
            when(productRepository.findByActiveTrueOrderByCreatedAtDesc())
                    .thenReturn(List.of());

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
            when(productRepository.findByIdAndActiveTrue(1L))
                    .thenReturn(Optional.of(testProduct));
            when(productMapper.toResponse(testProduct)).thenReturn(testProductResponse);

            // When
            ProductResponse result = productService.getProductById(1L);

            // Then
            assertThat(result).isNotNull();
            assertThat(result.getId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("должен выбросить ProductNotFoundException если товар не найден")
        void shouldThrowExceptionWhenProductNotFound() {
            // Given
            when(productRepository.findByIdAndActiveTrue(999L))
                    .thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> productService.getProductById(999L))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessageContaining("Product not found with id: 999");
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
            when(categoryRepository.existsById(1L)).thenReturn(true);
            when(productRepository.findByCategory_IdAndActiveTrue(1L))
                    .thenReturn(List.of(testProduct));
            when(productMapper.toResponse(testProduct)).thenReturn(testProductResponse);

            // When
            List<ProductResponse> result = productService.getProductsByCategory(1L);

            // Then
            assertThat(result).hasSize(1);
            assertThat(result.get(0).getCategoryId()).isEqualTo(1L);
        }

        @Test
        @DisplayName("должен выбросить исключение если категория не найдена")
        void shouldThrowExceptionWhenCategoryNotFound() {
            // Given
            when(categoryRepository.existsById(999L)).thenReturn(false);

            // When & Then
            assertThatThrownBy(() -> productService.getProductsByCategory(999L))
                    .isInstanceOf(InvalidProductException.class)
                    .hasMessageContaining("Category not found with id: 999");
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
            
            when(productRepository.findByActiveTrueAndNameContainingIgnoreCase("Test", pageable))
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
                    .id(2L)
                    .name("New Product")
                    .description("New Description")
                    .price(new BigDecimal("149.99"))
                    .stock(50)
                    .category(testCategory)
                    .active(true)
                    .build();

            ProductResponse newProductResponse = ProductResponse.builder()
                    .id(2L)
                    .name("New Product")
                    .description("New Description")
                    .price(new BigDecimal("149.99"))
                    .stock(50)
                    .categoryId(1L)
                    .active(true)
                    .build();

            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(productMapper.toEntity(testProductRequest)).thenReturn(newProduct);
            when(productRepository.save(any(Product.class))).thenReturn(newProduct);
            when(productMapper.toResponse(newProduct)).thenReturn(newProductResponse);

            // When
            ProductResponse result = productService.createProduct(testProductRequest);

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
            when(categoryRepository.findById(999L)).thenReturn(Optional.empty());
            testProductRequest.setCategoryId(999L);

            // When & Then
            assertThatThrownBy(() -> productService.createProduct(testProductRequest))
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
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(categoryRepository.findById(1L)).thenReturn(Optional.of(testCategory));
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);
            when(productMapper.toResponse(testProduct)).thenReturn(testProductResponse);

            // When
            ProductResponse result = productService.updateProduct(1L, testProductRequest);

            // Then
            assertThat(result).isNotNull();
            verify(productRepository).save(any(Product.class));
            verify(eventPublisher).publishProductEvent(any(ProductEvent.class), eq("product.updated"));
        }

        @Test
        @DisplayName("должен выбросить исключение если товар не найден")
        void shouldThrowExceptionWhenProductNotFoundOnUpdate() {
            // Given
            when(productRepository.findById(999L)).thenReturn(Optional.empty());

            // When & Then
            assertThatThrownBy(() -> productService.updateProduct(999L, testProductRequest))
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
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);

            // When
            productService.deleteProduct(1L);

            // Then
            assertThat(testProduct.getActive()).isFalse();
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
            // Given
            testProduct.setStock(100);
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
            when(productRepository.save(any(Product.class))).thenReturn(testProduct);

            // When
            productService.updateStock(1L, 10);

            // Then
            assertThat(testProduct.getStock()).isEqualTo(90);
            verify(eventPublisher).publishProductEvent(any(ProductEvent.class), eq("product.stock.updated"));
        }

        @Test
        @DisplayName("должен выбросить исключение при недостаточном остатке")
        void shouldThrowExceptionWhenInsufficientStock() {
            // Given
            testProduct.setStock(5);
            when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

            // When & Then
            assertThatThrownBy(() -> productService.updateStock(1L, 10))
                    .isInstanceOf(InvalidProductException.class)
                    .hasMessageContaining("Insufficient stock");
        }
    }

    // ========== GET TOP RATED PRODUCTS TESTS ==========

    @Nested
    @DisplayName("getTopRatedProducts()")
    class GetTopRatedProductsTests {

        @Test
        @DisplayName("должен вернуть топ товаров по рейтингу")
        void shouldReturnTopRatedProducts() {
            // Given
            when(productRepository.findTop10ByActiveTrueOrderByRatingDesc())
                    .thenReturn(List.of(testProduct));
            when(productMapper.toResponse(testProduct)).thenReturn(testProductResponse);

            // When
            List<ProductResponse> result = productService.getTopRatedProducts();

            // Then
            assertThat(result).isNotEmpty();
            assertThat(result).hasSize(1);
        }
    }

    // ========== GET PRODUCTS IN STOCK TESTS ==========

    @Nested
    @DisplayName("getProductsInStock()")
    class GetProductsInStockTests {

        @Test
        @DisplayName("должен вернуть товары в наличии")
        void shouldReturnProductsInStock() {
            // Given
            when(productRepository.findByActiveTrueAndStockGreaterThan(0))
                    .thenReturn(List.of(testProduct));
            when(productMapper.toResponse(testProduct)).thenReturn(testProductResponse);

            // When
            List<ProductResponse> result = productService.getProductsInStock();

            // Then
            assertThat(result).isNotEmpty();
            assertThat(result.get(0).getStock()).isGreaterThan(0);
        }
    }
}

