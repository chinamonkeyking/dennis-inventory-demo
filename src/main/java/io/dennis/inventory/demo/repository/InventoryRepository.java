package io.dennis.inventory.demo.repository;

import io.dennis.inventory.demo.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface InventoryRepository
        extends JpaRepository<Inventory, Integer>, JpaSpecificationExecutor<Inventory> {

    boolean existsByName(String name);
}
