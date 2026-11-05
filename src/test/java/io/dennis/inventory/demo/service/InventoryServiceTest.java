package io.dennis.inventory.demo.service;

import io.dennis.inventory.demo.api.BusinessException;
import io.dennis.inventory.demo.api.ErrorCode;
import io.dennis.inventory.demo.dto.CreateInventoryRequest;
import io.dennis.inventory.demo.dto.InventoryView;
import io.dennis.inventory.demo.dto.PageResult;
import io.dennis.inventory.demo.dto.QueryInventoryRequest;
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
    void returnsDefaultEmptyPageWhenNoInventoriesExist() {
        PageResult<InventoryView> result = inventoryService.query(null);

        assertThat(result.items()).isEmpty();
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalCount()).isZero();
    }

    @Test
    void returnsDefaultPageOrderedById() {
        Inventory first = saveInventory("Chocolate Cake", 1, 3, 10);
        Inventory second = saveInventory("Cheesecake", 1, 3, 5);

        PageResult<InventoryView> result = inventoryService.query(null);

        assertThat(result.items())
                .extracting(InventoryView::id)
                .containsExactly(first.getId(), second.getId());
        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.items().get(0).name()).isEqualTo("Chocolate Cake");
        assertThat(result.items().get(0).categoryName()).isEqualTo("Food");
        assertThat(result.items().get(0).subCategoryName()).isEqualTo("Cake");
    }

    @Test
    void filtersByCategoryAndSubCategory() {
        saveInventory("Chocolate Cake", 1, 3, 10);
        saveInventory("Cheesecake", 1, 3, 5);
        saveInventory("Running Shoe", 2, 4, 7);

        PageResult<InventoryView> byCategory = inventoryService.query(
                new QueryInventoryRequest(1, null, null, null, null, null));
        PageResult<InventoryView> bySubCategory = inventoryService.query(
                new QueryInventoryRequest(null, 4, null, null, null, null));
        PageResult<InventoryView> combined = inventoryService.query(
                new QueryInventoryRequest(1, 4, null, null, null, null));

        assertThat(byCategory.totalCount()).isEqualTo(2);
        assertThat(byCategory.items())
                .extracting(InventoryView::name)
                .containsExactly("Chocolate Cake", "Cheesecake");
        assertThat(bySubCategory.totalCount()).isEqualTo(1);
        assertThat(bySubCategory.items().get(0).name()).isEqualTo("Running Shoe");
        assertThat(combined.items()).isEmpty();
        assertThat(combined.totalCount()).isZero();
    }

    @Test
    void returnsEmptyPageForUnknownCategory() {
        PageResult<InventoryView> result = inventoryService.query(
                new QueryInventoryRequest(999, null, null, null, 2, 5));

        assertThat(result.items()).isEmpty();
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(5);
        assertThat(result.totalCount()).isZero();
    }

    @Test
    void sortsAndPagesResults() {
        saveInventory("Cake A", 1, 3, 10);
        saveInventory("Cake B", 1, 3, 5);
        saveInventory("Cake C", 1, 3, 7);

        PageResult<InventoryView> firstPage = inventoryService.query(
                new QueryInventoryRequest(null, null, "quantity", "desc", 1, 2));
        PageResult<InventoryView> secondPage = inventoryService.query(
                new QueryInventoryRequest(null, null, "quantity", "desc", 2, 2));

        assertThat(firstPage.items())
                .extracting(InventoryView::name)
                .containsExactly("Cake A", "Cake C");
        assertThat(firstPage.totalCount()).isEqualTo(3);
        assertThat(firstPage.page()).isEqualTo(1);
        assertThat(firstPage.size()).isEqualTo(2);
        assertThat(secondPage.items())
                .extracting(InventoryView::name)
                .containsExactly("Cake B");
        assertThat(secondPage.totalCount()).isEqualTo(3);
        assertThat(secondPage.page()).isEqualTo(2);
    }

    @Test
    void usesIdAsStableTieBreaker() {
        Inventory first = saveInventory("Cake A", 1, 3, 5);
        Inventory second = saveInventory("Cake B", 1, 3, 5);

        PageResult<InventoryView> result = inventoryService.query(
                new QueryInventoryRequest(null, null, "quantity", "ASC", 1, 20));

        assertThat(result.items())
                .extracting(InventoryView::id)
                .containsExactly(first.getId(), second.getId());
    }

    @Test
    void acceptsAllAllowedSortFields() {
        saveInventory("Cake A", 1, 3, 5);

        for (String sortBy : List.of(
                "id", "name", "quantity", "createdAtUtcMs", "updatedAtUtcMs")) {
            PageResult<InventoryView> result = inventoryService.query(
                    new QueryInventoryRequest(null, null, sortBy, "ASC", 1, 20));
            assertThat(result.items()).hasSize(1);
        }
    }

    @Test
    void returnsEmptyPageBeyondLastPage() {
        saveInventory("Cake A", 1, 3, 5);

        PageResult<InventoryView> result = inventoryService.query(
                new QueryInventoryRequest(null, null, null, null, 2, 1));

        assertThat(result.items()).isEmpty();
        assertThat(result.page()).isEqualTo(2);
        assertThat(result.size()).isEqualTo(1);
        assertThat(result.totalCount()).isEqualTo(1);
    }

    @Test
    void rejectsInvalidQueryParameters() {
        assertQueryBusinessError(
                new QueryInventoryRequest(0, null, null, null, null, null),
                ErrorCode.INVALID_REQUEST);
        assertQueryBusinessError(
                new QueryInventoryRequest(null, 0, null, null, null, null),
                ErrorCode.INVALID_REQUEST);
        assertQueryBusinessError(
                new QueryInventoryRequest(null, null, null, null, 0, null),
                ErrorCode.INVALID_REQUEST);
        assertQueryBusinessError(
                new QueryInventoryRequest(null, null, null, null, null, 0),
                ErrorCode.INVALID_REQUEST);
        assertQueryBusinessError(
                new QueryInventoryRequest(null, null, null, null, null, 101),
                ErrorCode.INVALID_REQUEST);
        assertQueryBusinessError(
                new QueryInventoryRequest(null, null, "price", "ASC", null, null),
                ErrorCode.INVALID_REQUEST);
        assertQueryBusinessError(
                new QueryInventoryRequest(null, null, null, "sideways", null, null),
                ErrorCode.INVALID_REQUEST);
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
    private Inventory saveInventory(String name, int categoryId, int subCategoryId, int quantity) {
        return inventoryRepository.saveAndFlush(new Inventory(
                name,
                categoryId,
                subCategoryId,
                quantity,
                "admin",
                Instant.now().toEpochMilli()));
    }

    private void assertQueryBusinessError(QueryInventoryRequest request, ErrorCode errorCode) {
        assertThatThrownBy(() -> inventoryService.query(request))
                .isInstanceOfSatisfying(BusinessException.class,
                        exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
    }

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
