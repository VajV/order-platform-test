package com.ecommerce.product.controller;

import com.ecommerce.product.dto.ProductCreateRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.dto.ProductUpdateRequest;
import com.ecommerce.product.service.ProductService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    /**
     * GET /api/products - Получить все товары
     */
    @GetMapping
    public ResponseEntity<List<ProductResponse>> getAllProducts() {
        log.info("GET /api/products - Get all products");
        return ResponseEntity.ok(productService.getAllProducts());
    }

    /**
     * GET /api/products/{id} - Получить товар по ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProductById(@PathVariable String id) {
        log.info("GET /api/products/{} - Get product by id", id);
        return ResponseEntity.ok(productService.getProductById(id));
    }

    /**
     * GET /api/products/category/{categoryId} - Товары по категории
     */
    @GetMapping("/category/{categoryId}")
    public ResponseEntity<Page<ProductResponse>> getByCategory(
            @PathVariable String categoryId,
            Pageable pageable
    ) {
        log.info("GET /api/products/category/{} - Get products by category", categoryId);
        return ResponseEntity.ok(productService.getProductsByCategory(categoryId, pageable));
    }

    /**
     * GET /api/products/search - Поиск товаров
     */
    @GetMapping("/search")
    public ResponseEntity<Page<ProductResponse>> searchProducts(
            @RequestParam String name,
            Pageable pageable
    ) {
        log.info("GET /api/products/search - Search products by name: {}", name);
        return ResponseEntity.ok(productService.searchProducts(name, pageable));
    }

    /**
     * GET /api/products/stock/available - Товары в наличии
     */
    @GetMapping("/stock/available")
    public ResponseEntity<List<ProductResponse>> getInStock() {
        log.info("GET /api/products/stock/available - Get products in stock");
        return ResponseEntity.ok(productService.getProductsInStock());
    }

    /**
     * GET /api/products/rating/top - Топ товаров
     */
    @GetMapping("/rating/top")
    public ResponseEntity<List<ProductResponse>> getTopRated() {
        log.info("GET /api/products/rating/top - Get top rated products");
        return ResponseEntity.ok(productService.getTopRatedProducts());
    }

    /**
     * POST /api/products - Создать товар (ADMIN ONLY)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> createProduct(@Valid @RequestBody ProductCreateRequest request) {
        log.info("POST /api/products - Create new product: {}", request.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.createProduct(request));
    }

    /**
     * PUT /api/products/{id} - Обновить товар (ADMIN ONLY)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ProductResponse> updateProduct(
            @PathVariable String id,
            @Valid @RequestBody ProductUpdateRequest request
    ) {
        log.info("PUT /api/products/{} - Update product", id);
        return ResponseEntity.ok(productService.updateProduct(id, request));
    }

    /**
     * DELETE /api/products/{id} - Удалить товар (ADMIN ONLY)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteProduct(@PathVariable String id) {
        log.info("DELETE /api/products/{} - Delete product", id);
        productService.deleteProduct(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/products/{id}/stock - Обновить количество (INTERNAL)
     */
    @PatchMapping("/{id}/stock")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> updateStock(
            @PathVariable String id,
            @RequestParam Integer quantity
    ) {
        log.info("PATCH /api/products/{}/stock - Update stock by: {}", id, quantity);
        productService.updateStock(id, quantity);
        return ResponseEntity.ok().build();
    }

    /**
     * PATCH /api/products/{id}/publish - Опубликовать товар (ADMIN ONLY)
     */
    @PatchMapping("/{id}/publish")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Publish product", description = "Make product visible in catalog")
    public ResponseEntity<ProductResponse> publishProduct(@PathVariable String id) {
        log.info("PATCH /api/products/{}/publish - Publish product", id);
        return ResponseEntity.ok(productService.publishProduct(id));
    }

    /**
     * PATCH /api/products/{id}/unpublish - Снять товар с публикации (ADMIN ONLY)
     */
    @PatchMapping("/{id}/unpublish")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Unpublish product", description = "Hide product from catalog")
    public ResponseEntity<ProductResponse> unpublishProduct(@PathVariable String id) {
        log.info("PATCH /api/products/{}/unpublish - Unpublish product", id);
        return ResponseEntity.ok(productService.unpublishProduct(id));
    }

    /**
     * GET /api/products/filter - Фильтр товаров по цене
     */
    @GetMapping("/filter")
    @Operation(summary = "Filter products by price range", description = "Get products within specified price range")
    public ResponseEntity<Page<ProductResponse>> filterByPrice(
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) String category,
            Pageable pageable
    ) {
        log.info("GET /api/products/filter - Filter by price: {} - {}, category: {}", minPrice, maxPrice, category);
        
        if (category != null && !category.isBlank()) {
            return ResponseEntity.ok(productService.filterByCategoryAndPriceRange(category, minPrice, maxPrice, pageable));
        }
        return ResponseEntity.ok(productService.filterByPriceRange(minPrice, maxPrice, pageable));
    }
}
