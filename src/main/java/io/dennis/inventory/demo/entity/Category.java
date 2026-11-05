package io.dennis.inventory.demo.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "category")
public class Category {

    @Id
    private Integer id;

    @Column(name = "parent_id")
    private Integer parentId;

    @Column(nullable = false, unique = true, length = 255)
    private String name;

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

    protected Category() {
        // Required by JPA.
    }

    public Integer getId() {
        return id;
    }

    public Integer getParentId() {
        return parentId;
    }

    public String getName() {
        return name;
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
}
