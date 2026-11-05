package io.dennis.inventory.demo.dto;

public record CreateInventoryRequest(
        String name,
        Integer categoryId,
        Integer subCategoryId,
        Integer quantity) {
}
