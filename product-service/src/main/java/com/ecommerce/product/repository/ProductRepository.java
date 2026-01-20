package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {

    // Найти все активные продукты
    List<Product> findByActiveTrueOrderByCreatedAtDesc();

    // Топ-10 продуктов по рейтингу
    List<Product> findTop10ByActiveTrueOrderByRatingDesc();

    // Продукты по категории
    List<Product> findByActiveTrueAndCategory_Id(Long categoryId);

    // Поиск по имени (регистронезависимый)
    Page<Product> findByActiveTrueAndNameContainingIgnoreCase(String name, Pageable pageable);

    // Продукты в наличии
    List<Product> findByActiveTrueAndStockGreaterThan(Integer stock);

    // Найти активный продукт по ID
    Optional<Product> findByIdAndActiveTrue(Long id);

    // Найти все активные продукты
    List<Product> findByActiveTrue();

    // Найти по категории и активности
    List<Product> findByCategory_IdAndActiveTrue(Long categoryId);

    // Поиск по диапазону цен
    List<Product> findByActiveTrueAndPriceBetween(java.math.BigDecimal minPrice, java.math.BigDecimal maxPrice);
}
