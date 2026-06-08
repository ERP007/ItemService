package com.fallguys.itemservice.domain;

import com.fallguys.itemservice.domain.exception.InvalidItemException;

public record UpdateItemSelectionCommand(
        String sku,
        String name,
        String categoryCode,
        String subCategoryCode,
        ItemUnit unit,
        int safetyStock,
        int unitPrice
) {

    public UpdateItemSelectionCommand {
        sku = requireText(sku, "sku");
        name = requireText(name, "name");
        categoryCode = requireText(categoryCode, "categoryCode");
        subCategoryCode = requireText(subCategoryCode, "subCategoryCode");
        if (unit == null) {
            throw new InvalidItemException("unit is required.");
        }
        if (safetyStock < 0) {
            throw new InvalidItemException("safetyStock must be greater than or equal to 0.");
        }
        if (unitPrice < 0) {
            throw new InvalidItemException("unitPrice must be greater than or equal to 0.");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidItemException(fieldName + " is required.");
        }
        return value.trim();
    }
}
