package io.dennis.inventory.demo.repository;

import io.dennis.inventory.demo.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<Category, Integer> {
}
