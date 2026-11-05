package io.dennis.inventory.demo.dto;

import io.dennis.inventory.demo.entity.Inventory;

public record InventoryView(
        Integer id,
        String name,
        Integer categoryId,
        String categoryName,
        Integer subCategoryId,
        String subCategoryName,
        int quantity,
        int version,
        String createdBy,
        long createdAtUtcMs,
        String updatedBy,
        Long updatedAtUtcMs) {

    public static InventoryView from(
            Inventory inventory,
            String categoryName,
            String subCategoryName) {
        return new InventoryView(
                inventory.getId(),
                inventory.getName(),
                inventory.getCategoryId(),
                categoryName,
                inventory.getSubCategoryId(),
                subCategoryName,
                inventory.getQuantity(),
                inventory.getVersion(),
                inventory.getCreatedBy(),
                inventory.getCreatedAtUtcMs(),
                inventory.getUpdatedBy(),
                inventory.getUpdatedAtUtcMs());
    }
}
