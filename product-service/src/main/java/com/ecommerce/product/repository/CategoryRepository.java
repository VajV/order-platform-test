package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Найти категорию по имени
    Optional<Category> findByName(String name);

    // Найти все активные категории
    List<Category> findByActiveTrue();

    // Проверить существование категории по имени
    boolean existsByName(String name);
}
