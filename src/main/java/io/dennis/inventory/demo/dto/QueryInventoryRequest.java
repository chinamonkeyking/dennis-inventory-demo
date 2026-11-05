package io.dennis.inventory.demo.dto;

public record QueryInventoryRequest(
        Integer categoryId,
        Integer subCategoryId,
        String sortBy,
        String sortDirection,
        Integer page,
        Integer size) {
}
