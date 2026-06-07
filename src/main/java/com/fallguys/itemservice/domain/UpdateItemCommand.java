package com.fallguys.itemservice.domain;

import com.fallguys.itemservice.domain.exception.InvalidItemException;

public record UpdateItemCommand(
        String sku,
        String name,
        String categoryCode,
        ItemUnit unit,
        int safetyStock,
        int unitPrice
) {

    public UpdateItemCommand {
        sku = requireText(sku, "sku");
        name = requireText(name, "name");
        categoryCode = requireText(categoryCode, "categoryCode");
        unit = requireUnit(unit);
        safetyStock = requireNonNegative(safetyStock, "safetyStock");
        unitPrice = requireNonNegative(unitPrice, "unitPrice");
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidItemException(fieldName + " is required.");
        }
        return value.trim();
    }

    private static ItemUnit requireUnit(ItemUnit unit) {
        if (unit == null) {
            throw new InvalidItemException("unit is required.");
        }
        return unit;
    }

    private static int requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new InvalidItemException(fieldName + " must be greater than or equal to 0.");
        }
        return value;
    }
}
