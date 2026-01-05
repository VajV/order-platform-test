package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    @Query("SELECT p FROM Product p WHERE p.active = true ORDER BY p.createdAt DESC")
    List<Product> findAllActive();

    @Query("SELECT p FROM Product p WHERE p.active = true ORDER BY p.rating DESC LIMIT 10")
    List<Product> findTopRatedProducts();

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.category.id = :categoryId")
    List<Product> findByCategory(@Param("categoryId") Long categoryId);

    @Query("SELECT p FROM Product p WHERE p.active = true AND LOWER(p.name) LIKE LOWER(CONCAT('%', :name, '%'))")
    Page<Product> searchByName(@Param("name") String name, Pageable pageable);

    @Query("SELECT p FROM Product p WHERE p.active = true AND p.stock > 0")
    List<Product> findInStock();

    Optional<Product> findByIdAndActiveTrue(Long id);
}