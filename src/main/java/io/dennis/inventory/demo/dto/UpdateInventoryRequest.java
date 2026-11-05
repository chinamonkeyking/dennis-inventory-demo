package io.dennis.inventory.demo.dto;

public record UpdateInventoryRequest(
        Integer id,
        Integer version,
        Integer quantity) {
}
