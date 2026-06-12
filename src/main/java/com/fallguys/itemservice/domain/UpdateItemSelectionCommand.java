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
            throw new InvalidItemException("단위는 필수입니다.");
        }
        if (safetyStock < 0) {
            throw new InvalidItemException("safetyStock은(는) 0 이상이어야 합니다.");
        }
        if (unitPrice < 0) {
            throw new InvalidItemException("unitPrice은(는) 0 이상이어야 합니다.");
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidItemException("필수값이 누락되었습니다: " + fieldName);
        }
        return value.trim();
    }
}
