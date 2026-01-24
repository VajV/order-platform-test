package com.ecommerce.product.repository;

import com.ecommerce.product.entity.Category;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends MongoRepository<Category, String> {

    // Найти категорию по имени
    Optional<Category> findByName(String name);

    // Проверить существование категории по имени
    boolean existsByName(String name);
}
