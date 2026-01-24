package com.ecommerce.product.service;

import com.ecommerce.product.dto.*;
import com.ecommerce.product.entity.Product;
import com.ecommerce.product.exception.InvalidProductException;
import com.ecommerce.product.exception.ProductNotFoundException;
import com.ecommerce.product.repository.CategoryRepository;
import com.ecommerce.product.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final ProductEventPublisher eventPublisher;

    /**
     * Получить все активные товары
     */
    public List<ProductResponse> getAllProducts() {
        log.info("Fetching all published products");
        Page<Product> products = productRepository.findByIsDeletedFalseAndIsPublishedTrue(Pageable.unpaged());
        log.debug("Found {} published products", products.getTotalElements());
        return products.getContent().stream().map(productMapper::toResponse).toList();
    }

    /**
     * Получить товар по ID
     */
    public ProductResponse getProductById(String id) {
        log.info("Fetching product with id: {}", id);

        Product product = productRepository.findByIdAndIsDeletedFalseAndIsPublishedTrue(id)
                .orElseThrow(() -> {
                    log.error("Product not found with id: {}", id);
                    return new ProductNotFoundException("Product not found with id: " + id);
                });

        log.debug("Found product: {}", product.getName());
        return productMapper.toResponse(product);
    }

    /**
     * Получить товары по категории
     */
    public Page<ProductResponse> getProductsByCategory(String category, Pageable pageable) {
        log.info("Fetching products for category: {}", category);

        validateCategoryExists(category);

        Page<Product> products = productRepository.findByIsDeletedFalseAndIsPublishedTrueAndCategory(category, pageable);
        log.debug("Found {} products in category {}", products.getTotalElements(), category);
        return products.map(productMapper::toResponse);
    }

    /**
     * Поиск товаров по названию
     */
    public Page<ProductResponse> searchProducts(String name, Pageable pageable) {
        log.info("Searching products by name: '{}', page: {}", name, pageable.getPageNumber());

        if (name == null || name.trim().isEmpty()) {
            log.error("Search term is empty");
            throw new InvalidProductException("Search term cannot be empty");
        }

        Page<Product> products = productRepository.findByIsDeletedFalseAndIsPublishedTrueAndNameContainingIgnoreCase(name.trim(), pageable);
        log.debug("Found {} products matching '{}'", products.getTotalElements(), name);

        return products.map(productMapper::toResponse);
    }

    /**
     * Получить товары в наличии
     */
    public List<ProductResponse> getProductsInStock() {
        throw new UnsupportedOperationException("Stock is managed by inventory-service");
    }

    /**
     * Получить топ товаров по рейтингу
     */
    public List<ProductResponse> getTopRatedProducts() {
        throw new UnsupportedOperationException("Ratings are not implemented in product-service");
    }

    /**
     * Создать новый товар (только ADMIN)
     */
    public ProductResponse createProduct(ProductCreateRequest request) {
        log.info("Creating new product: {}", request.getName());
        validateCategoryExists(request.getCategory());
        Product product = productMapper.toEntity(request);
        product.setIsPublished(false);
        product.setIsDeleted(false);

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with id: {}", savedProduct.getId());

        // Публикуем событие в Kafka
        publishProductCreatedEvent(savedProduct);

        return productMapper.toResponse(savedProduct);
    }

    /**
     * Обновить товар (только ADMIN)
     */
    public ProductResponse updateProduct(String id, ProductUpdateRequest request) {
        log.info("Updating product with id: {}", id);

        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> {
                    log.error("Product not found with id: {}", id);
                    return new ProductNotFoundException("Product not found with id: " + id);
                });

        if (request.getName() != null) {
            product.setName(request.getName());
        }
        if (request.getDescription() != null) {
            product.setDescription(request.getDescription());
        }
        if (request.getPrice() != null) {
            product.setPrice(request.getPrice());
        }
        if (request.getCategory() != null) {
            validateCategoryExists(request.getCategory());
            product.setCategory(request.getCategory());
        }
        if (request.getImageUrl() != null) {
            product.setImageUrl(request.getImageUrl());
        }

        Product updatedProduct = productRepository.save(product);
        log.info("Product updated successfully: {}", updatedProduct.getId());

        // Публикуем событие
        publishProductUpdatedEvent(updatedProduct);

        return productMapper.toResponse(updatedProduct);
    }

    /**
     * Удалить товар (только ADMIN) - soft delete
     */
    public void deleteProduct(String id) {
        log.info("Deleting product with id: {}", id);

        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> {
                    log.error("Product not found with id: {}", id);
                    return new ProductNotFoundException("Product not found with id: " + id);
                });

        product.setIsDeleted(true);
        product.setIsPublished(false);
        productRepository.save(product);

        log.info("Product marked as inactive (soft deleted): {}", id);

        // Публикуем событие
        publishProductDeletedEvent(product);
    }

    /**
     * Обновить количество товара (INTERNAL - вызывается из order-service)
     */
    public void updateStock(String id, Integer quantity) {
        throw new UnsupportedOperationException("Stock is managed by inventory-service");
    }

    /**
     * Опубликовать товар (сделать видимым в каталоге)
     */
    public ProductResponse publishProduct(String id) {
        log.info("Publishing product with id: {}", id);

        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> {
                    log.error("Product not found with id: {}", id);
                    return new ProductNotFoundException("Product not found with id: " + id);
                });

        if (product.getIsPublished()) {
            log.warn("Product {} is already published", id);
            return productMapper.toResponse(product);
        }

        product.setIsPublished(true);
        Product savedProduct = productRepository.save(product);
        log.info("Product published successfully: {}", id);

        publishProductUpdatedEvent(savedProduct);
        return productMapper.toResponse(savedProduct);
    }

    /**
     * Снять товар с публикации (скрыть из каталога)
     */
    public ProductResponse unpublishProduct(String id) {
        log.info("Unpublishing product with id: {}", id);

        Product product = productRepository.findByIdAndIsDeletedFalse(id)
                .orElseThrow(() -> {
                    log.error("Product not found with id: {}", id);
                    return new ProductNotFoundException("Product not found with id: " + id);
                });

        if (!product.getIsPublished()) {
            log.warn("Product {} is already unpublished", id);
            return productMapper.toResponse(product);
        }

        product.setIsPublished(false);
        Product savedProduct = productRepository.save(product);
        log.info("Product unpublished successfully: {}", id);

        publishProductUpdatedEvent(savedProduct);
        return productMapper.toResponse(savedProduct);
    }

    /**
     * Фильтр товаров по диапазону цен
     */
    public Page<ProductResponse> filterByPriceRange(BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        log.info("Filtering products by price range: {} - {}", minPrice, maxPrice);

        if (minPrice == null) {
            minPrice = BigDecimal.ZERO;
        }
        if (maxPrice == null) {
            maxPrice = new BigDecimal("999999999");
        }

        if (minPrice.compareTo(maxPrice) > 0) {
            throw new InvalidProductException("minPrice cannot be greater than maxPrice");
        }

        Page<Product> products = productRepository.findByIsDeletedFalseAndIsPublishedTrueAndPriceBetween(
                minPrice, maxPrice, pageable);
        log.debug("Found {} products in price range {} - {}", products.getTotalElements(), minPrice, maxPrice);

        return products.map(productMapper::toResponse);
    }

    /**
     * Фильтр товаров по категории и диапазону цен
     */
    public Page<ProductResponse> filterByCategoryAndPriceRange(
            String category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable) {
        log.info("Filtering products by category '{}' and price range: {} - {}", category, minPrice, maxPrice);

        validateCategoryExists(category);

        if (minPrice == null) {
            minPrice = BigDecimal.ZERO;
        }
        if (maxPrice == null) {
            maxPrice = new BigDecimal("999999999");
        }

        if (minPrice.compareTo(maxPrice) > 0) {
            throw new InvalidProductException("minPrice cannot be greater than maxPrice");
        }

        Page<Product> products = productRepository.findByIsDeletedFalseAndIsPublishedTrueAndCategoryAndPriceBetween(
                category, minPrice, maxPrice, pageable);
        log.debug("Found {} products in category '{}' with price range {} - {}",
                products.getTotalElements(), category, minPrice, maxPrice);

        return products.map(productMapper::toResponse);
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Проверка существования категории
     */
    private void validateCategoryExists(String category) {
        if (category == null || category.isBlank()) {
            throw new InvalidProductException("Category is required");
        }
        if (!categoryRepository.existsByName(category)) {
            log.error("Category not found with name: {}", category);
            throw new InvalidProductException("Category not found: " + category);
        }
    }

    /**
     * Публикация события создания товара
     */
    private void publishProductCreatedEvent(Product product) {
        ProductEvent event = ProductEvent.builder()
                .eventType("CREATED")
                .productId(product.getId())
                .productName(product.getName())
                .price(product.getPrice())
                .status(product.getIsPublished() ? "published" : "unpublished")
                .timestamp(LocalDateTime.now())
                .source("product-service")
                .build();

        eventPublisher.publishProductEvent(event, "product.created");
    }

    /**
     * Публикация события обновления товара
     */
    private void publishProductUpdatedEvent(Product product) {
        ProductEvent event = ProductEvent.builder()
                .eventType("UPDATED")
                .productId(product.getId())
                .productName(product.getName())
                .price(product.getPrice())
                .status(product.getIsPublished() ? "published" : "unpublished")
                .timestamp(LocalDateTime.now())
                .source("product-service")
                .build();

        eventPublisher.publishProductEvent(event, "product.updated");
    }

    /**
     * Публикация события удаления товара
     */
    private void publishProductDeletedEvent(Product product) {
        ProductEvent event = ProductEvent.builder()
                .eventType("DELETED")
                .productId(product.getId())
                .productName(product.getName())
                .status("deleted")
                .timestamp(LocalDateTime.now())
                .source("product-service")
                .build();

        eventPublisher.publishProductEvent(event, "product.deleted");
    }

    /**
     * Публикация события обновления остатков
     */
    private void publishStockUpdatedEvent(Product product) {
        ProductEvent event = ProductEvent.builder()
                .eventType("STOCK_UPDATED")
                .productId(product.getId())
                .productName(product.getName())
                .timestamp(LocalDateTime.now())
                .source("product-service")
                .build();

        eventPublisher.publishProductEvent(event, "product.stock.updated");
    }
}
