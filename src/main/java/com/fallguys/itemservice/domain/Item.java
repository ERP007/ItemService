package com.fallguys.itemservice.domain;

import com.fallguys.itemservice.domain.exception.InvalidItemException;
import com.fallguys.itemservice.domain.exception.InvalidItemStatusException;

import java.time.Instant;
import java.util.Objects;

public class Item {

    private final String sku;
    private String name;
    private String categoryCode;
    private ItemUnit unit;
    private int safetyStock;
    private int unitPrice;
    private boolean active;
    private final Instant createdAt;
    private Instant updatedAt;

    private Item(
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
        this.sku = requireText(sku, "sku");
        this.name = requireText(name, "name");
        this.categoryCode = requireText(categoryCode, "categoryCode");
        this.unit = requireUnit(unit);
        this.safetyStock = requireNonNegative(safetyStock, "safetyStock");
        this.unitPrice = requireNonNegative(unitPrice, "unitPrice");
        this.active = active;
        this.createdAt = requireInstant(createdAt, "createdAt");
        this.updatedAt = requireInstant(updatedAt, "updatedAt");
    }

    public static Item create(
            String sku,
            String name,
            String categoryCode,
            ItemUnit unit,
            int safetyStock,
            int unitPrice,
            Instant createdAt
    ) {
        return new Item(sku, name, categoryCode, unit, safetyStock, unitPrice, true, createdAt, createdAt);
    }

    public static Item of(
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
        return new Item(sku, name, categoryCode, unit, safetyStock, unitPrice, active, createdAt, updatedAt);
    }

    public void update(
            String name,
            String categoryCode,
            ItemUnit unit,
            int safetyStock,
            int unitPrice,
            Instant updatedAt
    ) {
        String validatedName = requireText(name, "name");
        String validatedCategoryCode = requireText(categoryCode, "categoryCode");
        ItemUnit validatedUnit = requireUnit(unit);
        int validatedSafetyStock = requireNonNegative(safetyStock, "safetyStock");
        int validatedUnitPrice = requireNonNegative(unitPrice, "unitPrice");
        Instant validatedUpdatedAt = requireInstant(updatedAt, "updatedAt");

        this.name = validatedName;
        this.categoryCode = validatedCategoryCode;
        this.unit = validatedUnit;
        this.safetyStock = validatedSafetyStock;
        this.unitPrice = validatedUnitPrice;
        this.updatedAt = validatedUpdatedAt;
    }

    public void activate(Instant updatedAt) {
        Instant validatedUpdatedAt = requireInstant(updatedAt, "updatedAt");
        if (active) {
            throw InvalidItemStatusException.alreadyActive(sku);
        }

        this.active = true;
        this.updatedAt = validatedUpdatedAt;
    }

    public void deactivate(Instant updatedAt) {
        Instant validatedUpdatedAt = requireInstant(updatedAt, "updatedAt");
        if (!active) {
            throw InvalidItemStatusException.alreadyInactive(sku);
        }

        this.active = false;
        this.updatedAt = validatedUpdatedAt;
    }

    public String getSku() {
        return sku;
    }

    public String getName() {
        return name;
    }

    public String getCategoryCode() {
        return categoryCode;
    }

    public ItemUnit getUnit() {
        return unit;
    }

    public int getSafetyStock() {
        return safetyStock;
    }

    public int getUnitPrice() {
        return unitPrice;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidItemException("필수값이 누락되었습니다: " + fieldName);
        }
        return value.trim();
    }

    private static ItemUnit requireUnit(ItemUnit unit) {
        if (unit == null) {
            throw new InvalidItemException("단위는 필수입니다.");
        }
        return unit;
    }

    private static int requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new InvalidItemException(fieldName + "은(는) 0 이상이어야 합니다.");
        }
        return value;
    }

    private static Instant requireInstant(Instant value, String fieldName) {
        if (value == null) {
            throw new InvalidItemException("필수값이 누락되었습니다: " + fieldName);
        }
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof Item item)) {
            return false;
        }
        return sku.equals(item.sku);
    }

    @Override
    public int hashCode() {
        return Objects.hash(sku);
    }
}
