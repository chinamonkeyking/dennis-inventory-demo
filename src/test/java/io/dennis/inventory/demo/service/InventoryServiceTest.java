package io.dennis.inventory.demo.service;

import io.dennis.inventory.demo.api.BusinessException;
import io.dennis.inventory.demo.api.ErrorCode;
import io.dennis.inventory.demo.dto.CreateInventoryRequest;
import io.dennis.inventory.demo.dto.InventoryView;
import io.dennis.inventory.demo.dto.UpdateInventoryRequest;
import io.dennis.inventory.demo.entity.Inventory;
import io.dennis.inventory.demo.repository.InventoryRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@Transactional
class InventoryServiceTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private InventoryRepository inventoryRepository;

    //------------------------------------------------------------------
    // query
    //------------------------------------------------------------------
    @Test
    void returnsEmptyListWhenNoInventoriesExist() {
        assertThat(inventoryService.query()).isEmpty();
    }

    @Test
    void returnsInventoriesOrderedById() {
        Inventory first = inventoryRepository.saveAndFlush(new Inventory(
                "Chocolate Cake", 1, 3, 10, "admin", Instant.now().toEpochMilli()));
        Inventory second = inventoryRepository.saveAndFlush(new Inventory(
                "Cheesecake", 1, 3, 5, "admin", Instant.now().toEpochMilli()));

        List<InventoryView> inventories = inventoryService.query();

        assertThat(inventories)
                .extracting(InventoryView::id)
                .containsExactly(first.getId(), second.getId());
        assertThat(inventories.get(0).name()).isEqualTo("Chocolate Cake");
        assertThat(inventories.get(0).categoryName()).isEqualTo("Food");
        assertThat(inventories.get(0).subCategoryName()).isEqualTo("Cake");
        assertThat(inventories.get(0).quantity()).isEqualTo(10);
        assertThat(inventories.get(1).name()).isEqualTo("Cheesecake");
        assertThat(inventories.get(1).categoryName()).isEqualTo("Food");
        assertThat(inventories.get(1).subCategoryName()).isEqualTo("Cake");
        assertThat(inventories.get(1).quantity()).isEqualTo(5);
    }

    //------------------------------------------------------------------
    // create
    //------------------------------------------------------------------
    @Test
    void createsInventoryWithServerManagedFields() {
        InventoryView created = inventoryService.create(
                new CreateInventoryRequest("  Raspberry Cake  ", 1, 3, 10));

        assertThat(created.id()).isPositive();
        assertThat(created.name()).isEqualTo("Raspberry Cake");
        assertThat(created.categoryId()).isEqualTo(1);
        assertThat(created.categoryName()).isEqualTo("Food");
        assertThat(created.subCategoryId()).isEqualTo(3);
        assertThat(created.subCategoryName()).isEqualTo("Cake");
        assertThat(created.quantity()).isEqualTo(10);
        assertThat(created.version()).isZero();
        assertThat(created.createdBy()).isEqualTo("admin");
        assertThat(created.createdAtUtcMs()).isPositive();
        assertThat(created.updatedBy()).isNull();
        assertThat(created.updatedAtUtcMs()).isNull();
    }

    @Test
    void rejectsInvalidFields() {
        // null params object
        assertBusinessError(null, ErrorCode.INVALID_REQUEST);

        // null name
        assertBusinessError(new CreateInventoryRequest(null, 1, 3, 10), ErrorCode.INVALID_REQUEST);

        // blank name
        assertBusinessError(new CreateInventoryRequest(" ", 1, 3, 10), ErrorCode.INVALID_REQUEST);

        // name exceeds the 255-character limit
        assertBusinessError(
                new CreateInventoryRequest("A".repeat(256), 1, 3, 10),
                ErrorCode.INVALID_REQUEST);

        // null quantity
        assertBusinessError(
                new CreateInventoryRequest("Raspberry Cake", 1, 3, null),
                ErrorCode.INVALID_REQUEST);

        // negative quantity
        assertBusinessError(
                new CreateInventoryRequest("Raspberry Cake", 1, 3, -1),
                ErrorCode.INVALID_REQUEST);

        // null categoryId
        assertBusinessError(
                new CreateInventoryRequest("Raspberry Cake", null, 3, 10),
                ErrorCode.INVALID_REQUEST);

        // null subCategoryId
        assertBusinessError(
                new CreateInventoryRequest("Raspberry Cake", 1, null, 10),
                ErrorCode.INVALID_REQUEST);
    }

    @Test
    void rejectsInvalidCategorySelection() {
        // category does not exist
        assertBusinessError(
                new CreateInventoryRequest("Raspberry Cake", 999, 3, 10),
                ErrorCode.INVALID_CATEGORY);

        // category is not top-level
        assertBusinessError(
                new CreateInventoryRequest("Raspberry Cake", 3, 3, 10),
                ErrorCode.INVALID_CATEGORY);

        // sub-category does not exist
        assertBusinessError(
                new CreateInventoryRequest("Raspberry Cake", 1, 999, 10),
                ErrorCode.INVALID_CATEGORY);

        // sub-category does not belong to the selected category
        assertBusinessError(
                new CreateInventoryRequest("Raspberry Cake", 1, 4, 10),
                ErrorCode.INVALID_CATEGORY);
    }

    @Test
    void rejectsDuplicateInventoryName() {
        // Create the inventory once.
        CreateInventoryRequest firstCreate =
                new CreateInventoryRequest("Blueberry Cake", 1, 3, 10);
        InventoryView created = inventoryService.create(firstCreate);
        assertThat(created.name()).isEqualTo("Blueberry Cake");

        // Creating another inventory with the same name must fail.
        CreateInventoryRequest duplicateCreate =
                new CreateInventoryRequest("Blueberry Cake", 1, 3, 10);

        assertBusinessError(duplicateCreate, ErrorCode.DUPLICATE_INVENTORY_NAME);
    }

    //------------------------------------------------------------------
    // update
    //------------------------------------------------------------------
    @Test
    void updatesOnlyQuantityAndIncrementsVersion() {
        InventoryView created = inventoryService.create(
                new CreateInventoryRequest("Pistachio Cake", 1, 3, 10));

        InventoryView updated = inventoryService.update(
                new UpdateInventoryRequest(created.id(), created.version(), 7));

        assertThat(updated.id()).isEqualTo(created.id());
        assertThat(updated.name()).isEqualTo(created.name());
        assertThat(updated.categoryId()).isEqualTo(created.categoryId());
        assertThat(updated.categoryName()).isEqualTo(created.categoryName());
        assertThat(updated.subCategoryId()).isEqualTo(created.subCategoryId());
        assertThat(updated.subCategoryName()).isEqualTo(created.subCategoryName());
        assertThat(updated.quantity()).isEqualTo(7);
        assertThat(updated.version()).isEqualTo(1);
        assertThat(updated.createdBy()).isEqualTo(created.createdBy());
        assertThat(updated.createdAtUtcMs()).isEqualTo(created.createdAtUtcMs());
        assertThat(updated.updatedBy()).isEqualTo("admin");
        assertThat(updated.updatedAtUtcMs()).isNotNull().isPositive();
    }

    @Test
    void rejectsStaleVersionWithoutChangingData() {
        InventoryView created = inventoryService.create(
                new CreateInventoryRequest("Walnut Cake", 1, 3, 10));
        inventoryService.update(
                new UpdateInventoryRequest(created.id(), created.version(), 7));

        assertUpdateBusinessError(
                new UpdateInventoryRequest(created.id(), created.version(), 5),
                ErrorCode.VERSION_CONFLICT);

        Inventory stored = inventoryRepository.findById(created.id()).orElseThrow();
        assertThat(stored.getQuantity()).isEqualTo(7);
        assertThat(stored.getVersion()).isEqualTo(1);
    }

    @Test
    void rejectsUpdateForMissingInventory() {
        assertUpdateBusinessError(
                new UpdateInventoryRequest(9999, 0, 5),
                ErrorCode.INVENTORY_NOT_FOUND);
    }

    @Test
    void rejectsInvalidUpdateFields() {
        // null id
        assertUpdateBusinessError(
                new UpdateInventoryRequest(null, 0, 5),
                ErrorCode.INVALID_REQUEST);

        // null version
        assertUpdateBusinessError(
                new UpdateInventoryRequest(1, null, 5),
                ErrorCode.INVALID_REQUEST);

        // negative version
        assertUpdateBusinessError(
                new UpdateInventoryRequest(1, -1, 5),
                ErrorCode.INVALID_REQUEST);

        // null quantity
        assertUpdateBusinessError(
                new UpdateInventoryRequest(1, 0, null),
                ErrorCode.INVALID_REQUEST);

        // negative quantity
        assertUpdateBusinessError(
                new UpdateInventoryRequest(1, 0, -1),
                ErrorCode.INVALID_REQUEST);
    }

    //------------------------------------------------------------------
    // helper functions
    //------------------------------------------------------------------
    private void assertBusinessError(CreateInventoryRequest request, ErrorCode errorCode) {
        assertThatThrownBy(() -> inventoryService.create(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }

    private void assertUpdateBusinessError(UpdateInventoryRequest request, ErrorCode errorCode) {
        assertThatThrownBy(() -> inventoryService.update(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }
}
