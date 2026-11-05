package io.dennis.inventory.demo.repository;

import io.dennis.inventory.demo.entity.Inventory;
import jakarta.persistence.EntityManager;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Transactional
class InventoryRepositoryTest {

    @Autowired
    private InventoryRepository inventoryRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    void savesInventoryWithGeneratedIdAndInitialVersionZero() {
        String name = "Chocolate Cake";
        int categoryId = 1;
        int subCategoryId = 3;
        int quantity = 10;
        String createdBy = "admin";
        long createdAtUtcMs = Instant.now().toEpochMilli();

        Inventory inventory = new Inventory(
                name,
                categoryId,
                subCategoryId,
                quantity,
                createdBy,
                createdAtUtcMs);

        Inventory saved = inventoryRepository.saveAndFlush(inventory);

        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getId()).isPositive();
        assertThat(saved.getVersion()).isZero(); // version is 0 after creation

        // Clear the persistence context so findById reloads from the database,
        // not from Hibernate's first-level cache.
        entityManager.clear();

        Inventory reloaded = inventoryRepository.findById(saved.getId()).orElseThrow();
        assertThat(reloaded.getName()).isEqualTo(name);
        assertThat(reloaded.getCategoryId()).isEqualTo(categoryId);
        assertThat(reloaded.getSubCategoryId()).isEqualTo(subCategoryId);
        assertThat(reloaded.getQuantity()).isEqualTo(quantity);
        assertThat(reloaded.getVersion()).isZero();
        assertThat(reloaded.getCreatedBy()).isEqualTo(createdBy);
        assertThat(reloaded.getCreatedAtUtcMs()).isEqualTo(createdAtUtcMs);
        assertThat(reloaded.getUpdatedBy()).isNull();
        assertThat(reloaded.getUpdatedAtUtcMs()).isNull();
    }
}
