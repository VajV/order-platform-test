package com.ecommerce.product.service;

import com.ecommerce.product.dto.*;
import com.ecommerce.product.entity.Category;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductMapper productMapper;
    private final ProductEventPublisher eventPublisher;

    /**
     * Получить все активные товары
     */
    public List<ProductResponse> getAllProducts() {
        log.info("Fetching all active products");
        List<Product> products = productRepository.findAllActive();
        log.debug("Found {} active products", products.size());

        return products.stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Получить товар по ID
     */
    public ProductResponse getProductById(Long id) {
        log.info("Fetching product with id: {}", id);

        Product product = productRepository.findByIdAndActiveTrue(id)
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
    public List<ProductResponse> getProductsByCategory(Long categoryId) {
        log.info("Fetching products for category id: {}", categoryId);

        // Проверяем существование категории
        validateCategoryExists(categoryId);

        List<Product> products = productRepository.findByCategory(categoryId);
        log.debug("Found {} products in category {}", products.size(), categoryId);

        return products.stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
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

        Page<Product> products = productRepository.searchByName(name.trim(), pageable);
        log.debug("Found {} products matching '{}'", products.getTotalElements(), name);

        return products.map(productMapper::toResponse);
    }

    /**
     * Получить товары в наличии
     */
    public List<ProductResponse> getProductsInStock() {
        log.info("Fetching products in stock");

        List<Product> products = productRepository.findInStock();
        log.debug("Found {} products in stock", products.size());

        return products.stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Получить топ товаров по рейтингу
     */
    public List<ProductResponse> getTopRatedProducts() {
        log.info("Fetching top rated products");

        List<Product> products = productRepository.findTopRatedProducts();
        log.debug("Found {} top rated products", products.size());

        return products.stream()
                .map(productMapper::toResponse)
                .collect(Collectors.toList());
    }

    /**
     * Создать новый товар (только ADMIN)
     */
    @Transactional
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating new product: {}", request.getName());

        // Валидация категории
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> {
                    log.error("Category not found with id: {}", request.getCategoryId());
                    return new InvalidProductException("Category not found with id: " + request.getCategoryId());
                });

        // Создание товара
        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .category(category)
                .imageUrl(request.getImageUrl())
                .active(request.isActive())
                .rating(0.0)
                .reviewCount(0)
                .build();

        Product savedProduct = productRepository.save(product);
        log.info("Product created successfully with id: {}", savedProduct.getId());

        // Публикуем событие в Kafka
        publishProductCreatedEvent(savedProduct);

        return productMapper.toResponse(savedProduct);
    }

    /**
     * Обновить товар (только ADMIN)
     */
    @Transactional
    public ProductResponse updateProduct(Long id, ProductRequest request) {
        log.info("Updating product with id: {}", id);

        // Находим существующий товар
        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Product not found with id: {}", id);
                    return new ProductNotFoundException("Product not found with id: " + id);
                });

        // Валидация категории
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> {
                    log.error("Category not found with id: {}", request.getCategoryId());
                    return new InvalidProductException("Category not found with id: " + request.getCategoryId());
                });

        // Обновление полей
        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setCategory(category);
        product.setImageUrl(request.getImageUrl());
        product.setActive(request.isActive());

        Product updatedProduct = productRepository.save(product);
        log.info("Product updated successfully: {}", updatedProduct.getId());

        // Публикуем событие
        publishProductUpdatedEvent(updatedProduct);

        return productMapper.toResponse(updatedProduct);
    }

    /**
     * Удалить товар (только ADMIN) - soft delete
     */
    @Transactional
    public void deleteProduct(Long id) {
        log.info("Deleting product with id: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Product not found with id: {}", id);
                    return new ProductNotFoundException("Product not found with id: " + id);
                });

        // Soft delete - помечаем как неактивный
        product.setActive(false);
        productRepository.save(product);

        log.info("Product marked as inactive (soft deleted): {}", id);

        // Публикуем событие
        publishProductDeletedEvent(product);
    }

    /**
     * Обновить количество товара (INTERNAL - вызывается из order-service)
     */
    @Transactional
    public void updateStock(Long id, Integer quantity) {
        log.info("Updating stock for product {}: reduce by {}", id, quantity);

        Product product = productRepository.findById(id)
                .orElseThrow(() -> {
                    log.error("Product not found with id: {}", id);
                    return new ProductNotFoundException("Product not found with id: " + id);
                });

        // Проверка наличия
        if (product.getStock() < quantity) {
            log.error("Insufficient stock for product {}. Available: {}, Requested: {}",
                    id, product.getStock(), quantity);
            throw new InvalidProductException(
                    String.format("Insufficient stock. Available: %d, Requested: %d",
                            product.getStock(), quantity)
            );
        }

        // Уменьшаем количество
        product.setStock(product.getStock() - quantity);
        productRepository.save(product);

        log.info("Stock updated successfully for product {}. New stock: {}", id, product.getStock());

        // Публикуем событие
        publishStockUpdatedEvent(product);
    }

    /**
     * Резервирование товара (для будущей реализации)
     */
    @Transactional
    public void reserveStock(Long id, Integer quantity) {
        log.info("Reserving stock for product {}: {}", id, quantity);

        // TODO: Реализовать отдельную таблицу reservations
        // TODO: Создать запись о резервировании
        // TODO: Установить timeout для резервирования (например, 10 минут)

        log.warn("Stock reservation not implemented yet");
    }

    /**
     * Отменить резервирование (для будущей реализации)
     */
    @Transactional
    public void cancelReservation(Long reservationId) {
        log.info("Cancelling reservation: {}", reservationId);

        // TODO: Найти резервирование
        // TODO: Вернуть товар на склад
        // TODO: Удалить запись о резервировании

        log.warn("Reservation cancellation not implemented yet");
    }

    // ========== PRIVATE HELPER METHODS ==========

    /**
     * Проверка существования категории
     */
    private void validateCategoryExists(Long categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            log.error("Category not found with id: {}", categoryId);
            throw new InvalidProductException("Category not found with id: " + categoryId);
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
                .stock(product.getStock())
                .status("active")
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
                .stock(product.getStock())
                .status(product.isActive() ? "active" : "inactive")
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
                .stock(product.getStock())
                .timestamp(LocalDateTime.now())
                .source("product-service")
                .build();

        eventPublisher.publishProductEvent(event, "product.stock.updated");
    }
}
