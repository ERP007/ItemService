package com.fallguys.itemservice.domain;

import com.fallguys.itemservice.domain.exception.InvalidItemException;

public record CreateItemCommand(
        String sku,
        String name,
        String categoryCode,
        ItemUnit unit,
        int safetyStock,
        int unitPrice
) {

    public CreateItemCommand {
        sku = requireText(sku, "sku");
        name = requireText(name, "name");
        categoryCode = requireText(categoryCode, "categoryCode");
        unit = requireUnit(unit);
        safetyStock = requireNonNegative(safetyStock, "safetyStock");
        unitPrice = requireNonNegative(unitPrice, "unitPrice");
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
}
