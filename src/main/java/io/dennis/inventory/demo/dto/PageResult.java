package io.dennis.inventory.demo.dto;

import java.util.List;

public record PageResult<T>(
        List<T> items,
        int page,
        int size,
        long totalCount) {
}
