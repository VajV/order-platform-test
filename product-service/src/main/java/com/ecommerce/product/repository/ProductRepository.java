package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    Page<Product> findByIsDeletedFalseAndIsPublishedTrue(Pageable pageable);

    Page<Product> findByIsDeletedFalseAndIsPublishedTrueAndCategory(String category, Pageable pageable);

    Page<Product> findByIsDeletedFalseAndIsPublishedTrueAndNameContainingIgnoreCase(String name, Pageable pageable);

    Optional<Product> findByIdAndIsDeletedFalseAndIsPublishedTrue(String id);

    Optional<Product> findByIdAndIsDeletedFalse(String id);

    // Price range filter
    Page<Product> findByIsDeletedFalseAndIsPublishedTrueAndPriceBetween(
            BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);

    // Price range with category
    Page<Product> findByIsDeletedFalseAndIsPublishedTrueAndCategoryAndPriceBetween(
            String category, BigDecimal minPrice, BigDecimal maxPrice, Pageable pageable);
}
