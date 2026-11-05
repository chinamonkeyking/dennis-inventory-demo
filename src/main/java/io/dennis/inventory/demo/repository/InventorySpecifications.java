package io.dennis.inventory.demo.repository;

import io.dennis.inventory.demo.entity.Inventory;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.jpa.domain.Specification;

import java.util.ArrayList;
import java.util.List;

public final class InventorySpecifications {

    private InventorySpecifications() {
    }

    public static Specification<Inventory> matching(Integer categoryId, Integer subCategoryId) {
        return (root, query, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (categoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("categoryId"), categoryId));
            }
            if (subCategoryId != null) {
                predicates.add(criteriaBuilder.equal(root.get("subCategoryId"), subCategoryId));
            }
            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }
}
