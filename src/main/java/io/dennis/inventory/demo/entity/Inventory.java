package io.dennis.inventory.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "inventory")
public class Inventory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

    @Column(name = "category_id", nullable = false)
    private Integer categoryId;

    @Column(name = "sub_category_id", nullable = false)
    private Integer subCategoryId;

    @Column(nullable = false)
    private int quantity;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_by", nullable = false, length = 255)
    private String createdBy;

    @Column(name = "created_at_utc_ms", nullable = false)
    private long createdAtUtcMs;

    @Column(name = "updated_by", length = 255)
    private String updatedBy;

    @Column(name = "updated_at_utc_ms")
    private Long updatedAtUtcMs;

    protected Inventory() {
        // Required by JPA.
    }

    public Inventory(
            String name,
            Integer categoryId,
            Integer subCategoryId,
            int quantity,
            String createdBy,
            long createdAtUtcMs) {
        this.name = name;
        this.categoryId = categoryId;
        this.subCategoryId = subCategoryId;
        this.quantity = quantity;
        this.createdBy = createdBy;
        this.createdAtUtcMs = createdAtUtcMs;
    }

    public Integer getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Integer getCategoryId() {
        return categoryId;
    }

    public Integer getSubCategoryId() {
        return subCategoryId;
    }

    public int getQuantity() {
        return quantity;
    }

    public int getVersion() {
        return version;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public long getCreatedAtUtcMs() {
        return createdAtUtcMs;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public Long getUpdatedAtUtcMs() {
        return updatedAtUtcMs;
    }

    public void updateQuantity(int quantity, String updatedBy, long updatedAtUtcMs) {
        this.quantity = quantity;
        this.updatedBy = updatedBy;
        this.updatedAtUtcMs = updatedAtUtcMs;
    }
}
