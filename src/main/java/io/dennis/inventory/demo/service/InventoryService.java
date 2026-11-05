package io.dennis.inventory.demo.service;

import io.dennis.inventory.demo.api.BusinessException;
import io.dennis.inventory.demo.api.ErrorCode;
import io.dennis.inventory.demo.dto.CreateInventoryRequest;
import io.dennis.inventory.demo.dto.InventoryView;
import io.dennis.inventory.demo.dto.PageResult;
import io.dennis.inventory.demo.dto.QueryInventoryRequest;
import io.dennis.inventory.demo.dto.UpdateInventoryRequest;
import io.dennis.inventory.demo.entity.Category;
import io.dennis.inventory.demo.entity.Inventory;
import io.dennis.inventory.demo.repository.CategoryRepository;
import io.dennis.inventory.demo.repository.InventoryRepository;
import io.dennis.inventory.demo.repository.InventorySpecifications;
import io.dennis.inventory.demo.util.ValidationUtil;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class InventoryService {

    // Since we don't have user authentication, so hardcode a user by now
    private static final String DEFAULT_ACTOR = "admin";
    private static final int MAX_NAME_LENGTH = 255;
    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_PAGE_SIZE = 20;
    private static final int MAX_PAGE_SIZE = 100;
    private static final String DEFAULT_SORT_FIELD = "id";
    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
            "id", "name", "quantity", "createdAtUtcMs", "updatedAtUtcMs");

    private final InventoryRepository inventoryRepository;
    private final CategoryRepository categoryRepository;

    public InventoryService(
            InventoryRepository inventoryRepository,
            CategoryRepository categoryRepository) {
        this.inventoryRepository = inventoryRepository;
        this.categoryRepository = categoryRepository;
    }

    @Transactional(readOnly = true)
    public PageResult<InventoryView> query(QueryInventoryRequest request) {
        QueryInventoryRequest query = request == null
                ? new QueryInventoryRequest(null, null, null, null, null, null)
                : request;

        Integer categoryId = ValidationUtil.optionalPositiveInteger(query.categoryId(), "categoryId");
        Integer subCategoryId = ValidationUtil.optionalPositiveInteger(
                query.subCategoryId(), "subCategoryId");
        int page = query.page() == null
                ? DEFAULT_PAGE
                : ValidationUtil.requirePositiveInteger(query.page(), "page");
        int size = query.size() == null
                ? DEFAULT_PAGE_SIZE
                : ValidationUtil.requireIntegerInRange(query.size(), "size", 1, MAX_PAGE_SIZE);

        String sortField = requireSortField(query.sortBy());
        Sort.Direction direction = requireSortDirection(query.sortDirection());
        Sort sort = Sort.by(direction, sortField);
        if (!DEFAULT_SORT_FIELD.equals(sortField)) {
            sort = sort.and(Sort.by(Sort.Direction.ASC, DEFAULT_SORT_FIELD));
        }

        Specification<Inventory> specification =
                InventorySpecifications.matching(categoryId, subCategoryId);
        Pageable pageable = PageRequest.of(page - 1, size, sort);
        Page<Inventory> inventoryPage = inventoryRepository.findAll(specification, pageable);
        List<Inventory> inventories = inventoryPage.getContent();
        long totalCount = inventoryPage.getTotalElements();

        if (inventories.isEmpty()) {
            return new PageResult<>(List.of(), page, size, totalCount);
        }

        Set<Integer> categoryIds = inventories.stream()
                .flatMap(inventory -> Stream.of(inventory.getCategoryId(), inventory.getSubCategoryId()))
                .collect(Collectors.toSet());

        Map<Integer, String> categoryNames = findCategoryNames(categoryIds);
        List<InventoryView> items = inventories.stream()
                .map(inventory -> InventoryView.from(
                        inventory,
                        categoryNames.get(inventory.getCategoryId()),
                        categoryNames.get(inventory.getSubCategoryId())))
                .toList();

        return new PageResult<>(items, page, size, totalCount);
    }

    private String requireSortField(String sortBy) {
        if (sortBy == null || sortBy.isBlank()) {
            return DEFAULT_SORT_FIELD;
        }

        String normalized = sortBy.trim();
        if (!ALLOWED_SORT_FIELDS.contains(normalized)) {
            throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "sortBy must be one of: id, name, quantity, createdAtUtcMs, updatedAtUtcMs");
        }
        return normalized;
    }

    private Sort.Direction requireSortDirection(String sortDirection) {
        if (sortDirection == null || sortDirection.isBlank()) {
            return Sort.Direction.ASC;
        }

        return switch (sortDirection.trim().toUpperCase(Locale.ROOT)) {
            case "ASC" -> Sort.Direction.ASC;
            case "DESC" -> Sort.Direction.DESC;
            default -> throw new BusinessException(
                    ErrorCode.INVALID_REQUEST,
                    "sortDirection must be ASC or DESC");
        };
    }

    @Transactional
    public InventoryView create(CreateInventoryRequest request) {
        //------------------------
        // 1. Validation
        //------------------------
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "params are required");
        }

        String name = ValidationUtil.requireString(request.name(), "name", MAX_NAME_LENGTH);
        int quantity = ValidationUtil.requireNonNegativeInteger(request.quantity(), "quantity");
        int categoryId = ValidationUtil.requirePositiveInteger(request.categoryId(), "categoryId");
        int subCategoryId = ValidationUtil.requirePositiveInteger(
                request.subCategoryId(), "subCategoryId");

        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_CATEGORY,
                        "category " + categoryId + " does not exist"));
        if (category.getParentId() != null) {
            throw new BusinessException(
                    ErrorCode.INVALID_CATEGORY,
                    "category " + categoryId + " is not a top-level category");
        }

        Category subCategory = categoryRepository.findById(subCategoryId)
                .orElseThrow(() -> new BusinessException(
                        ErrorCode.INVALID_CATEGORY,
                        "sub-category " + subCategoryId + " does not exist"));
        if (!Objects.equals(subCategory.getParentId(), categoryId)) {
            throw new BusinessException(
                    ErrorCode.INVALID_CATEGORY,
                    "sub-category " + subCategoryId + " does not belong to category " + categoryId);
        }

        if (inventoryRepository.existsByName(name)) {
            throw new BusinessException(ErrorCode.DUPLICATE_INVENTORY_NAME, name);
        }

        //------------------------
        // 2. Create and return
        //------------------------
        Inventory inventory = new Inventory(
                name,
                categoryId,
                subCategoryId,
                quantity,
                DEFAULT_ACTOR,
                Instant.now().toEpochMilli());

        Inventory saved = inventoryRepository.saveAndFlush(inventory);
        return InventoryView.from(saved, category.getName(), subCategory.getName());
    }

    @Transactional
    public InventoryView update(UpdateInventoryRequest request) {
        //------------------------
        // 1. Validation
        //------------------------
        if (request == null) {
            throw new BusinessException(ErrorCode.INVALID_REQUEST, "params are required");
        }

        int id = ValidationUtil.requirePositiveInteger(request.id(), "id");
        int version = ValidationUtil.requireNonNegativeInteger(request.version(), "version");
        int quantity = ValidationUtil.requireNonNegativeInteger(request.quantity(), "quantity");

        Inventory inventory = inventoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.INVENTORY_NOT_FOUND, id));
        // Fast fail for a request that is already known to be based on a stale version.
        // The real concurrency guarantee comes from JPA @Version when the row is updated.
        if (inventory.getVersion() != version) {
            throw new BusinessException(ErrorCode.VERSION_CONFLICT);
        }

        //------------------------
        // 2. Update ('optimized lock' delegates to @version
        //------------------------
        inventory.updateQuantity(quantity, DEFAULT_ACTOR, Instant.now().toEpochMilli());
        // Because Inventory.version is annotated with @Version, Hibernate includes it in
        // UPDATE ... WHERE version = ? and increments it automatically. If another transaction
        // updated the row first, this flush affects 0 rows and Spring translates the
        // optimistic-lock failure to ObjectOptimisticLockingFailureException; the global
        // exception handler maps that exception to correct error code.
        Inventory saved = inventoryRepository.saveAndFlush(inventory);
        return toView(saved);
    }

    private InventoryView toView(Inventory inventory) {
        Map<Integer, String> categoryNames = findCategoryNames(
                Set.of(inventory.getCategoryId(), inventory.getSubCategoryId()));
        return InventoryView.from(
                inventory,
                categoryNames.get(inventory.getCategoryId()),
                categoryNames.get(inventory.getSubCategoryId()));
    }

    private Map<Integer, String> findCategoryNames(Set<Integer> categoryIds) {
        return categoryRepository.findAllById(categoryIds).stream()
                .collect(Collectors.toMap(Category::getId, Category::getName));
    }
}
