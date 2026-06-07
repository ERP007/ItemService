package com.fallguys.itemservice.infrastructure.persistence;

import com.fallguys.itemservice.domain.ItemCategory;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "item_categories")
public class ItemCategoryEntity {

    @Id
    @Column(name = "code", length = 64)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "parent_code", length = 64)
    private String parentCode;

    @Column(name = "depth", nullable = false)
    private int depth;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "active", nullable = false)
    private boolean active;

    protected ItemCategoryEntity() {
    }

    private ItemCategoryEntity(String code, String name, String parentCode, int depth, int displayOrder, boolean active) {
        this.code = code;
        this.name = name;
        this.parentCode = parentCode;
        this.depth = depth;
        this.displayOrder = displayOrder;
        this.active = active;
    }

    public static ItemCategoryEntity from(ItemCategory category) {
        return new ItemCategoryEntity(
                category.getCode(),
                category.getName(),
                category.getParentCode(),
                category.getDepth(),
                category.getDisplayOrder(),
                category.isActive()
        );
    }

    public ItemCategory toDomain() {
        return ItemCategory.of(code, name, parentCode, depth, displayOrder, active);
    }

    public ItemCategoryEntity update(ItemCategory category) {
        this.name = category.getName();
        this.parentCode = category.getParentCode();
        this.depth = category.getDepth();
        this.displayOrder = category.getDisplayOrder();
        this.active = category.isActive();
        return this;
    }
}
