package com.fallguys.itemservice.infrastructure.persistence;

import com.fallguys.itemservice.domain.Item;
import com.fallguys.itemservice.domain.ItemUnit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import java.time.Instant;

@Entity
@Table(name = "items")
public class ItemEntity {

    @Id
    @Column(name = "sku", length = 64)
    private String sku;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "category_code", nullable = false, length = 64)
    private String categoryCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "unit", nullable = false, length = 16)
    private ItemUnit unit;

    @Column(name = "safety_stock", nullable = false)
    private int safetyStock;

    @Column(name = "unit_price", nullable = false)
    private int unitPrice;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    protected ItemEntity() {
    }

    private ItemEntity(
            String sku,
            String name,
            String categoryCode,
            ItemUnit unit,
            int safetyStock,
            int unitPrice,
            boolean active,
            Instant createdAt,
            Instant updatedAt
    ) {
        this.sku = sku;
        this.name = name;
        this.categoryCode = categoryCode;
        this.unit = unit;
        this.safetyStock = safetyStock;
        this.unitPrice = unitPrice;
        this.active = active;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public static ItemEntity from(Item item) {
        return new ItemEntity(
                item.getSku(),
                item.getName(),
                item.getCategoryCode(),
                item.getUnit(),
                item.getSafetyStock(),
                item.getUnitPrice(),
                item.isActive(),
                item.getCreatedAt(),
                item.getUpdatedAt()
        );
    }

    public Item toDomain() {
        return Item.of(sku, name, categoryCode, unit, safetyStock, unitPrice, active, createdAt, updatedAt);
    }

    public ItemEntity update(Item item) {
        this.name = item.getName();
        this.categoryCode = item.getCategoryCode();
        this.unit = item.getUnit();
        this.safetyStock = item.getSafetyStock();
        this.unitPrice = item.getUnitPrice();
        this.active = item.isActive();
        this.updatedAt = item.getUpdatedAt();
        return this;
    }
}
