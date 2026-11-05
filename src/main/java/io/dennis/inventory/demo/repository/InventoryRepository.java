package io.dennis.inventory.demo.repository;

import io.dennis.inventory.demo.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryRepository extends JpaRepository<Inventory, Integer> {

    boolean existsByName(String name);
}
