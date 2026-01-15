package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface ProductRepository extends MongoRepository<Product, String> {

    // Найти все активные продукты
    List<Product> findByActiveTrueOrderByCreatedAtDesc();

    // Топ-10 продуктов по рейтингу
    List<Product> findTop10ByActiveTrueOrderByRatingDesc();

    // Продукты по категории
    List<Product> findByActiveTrueAndCategoryId(String categoryId);

    // Поиск по имени (регистронезависимый)
    @Query("{ 'active': true, 'name': { $regex: ?0, $options: 'i' } }")
    Page<Product> searchByName(String name, Pageable pageable);

    // Продукты в наличии
    @Query("{ 'active': true, 'stock': { $gt: 0 } }")
    List<Product> findInStock();

    // Найти активный продукт по ID
    Optional<Product> findByIdAndActiveTrue(String id);

    // Найти все активные продукты
    List<Product> findByActiveTrue();

    // Найти по категории и активности
    List<Product> findByCategoryIdAndActiveTrue(String categoryId);

    // Поиск по диапазону цен
    @Query("{ 'active': true, 'price': { $gte: ?0, $lte: ?1 } }")
    List<Product> findByPriceRange(Double minPrice, Double maxPrice);
}
