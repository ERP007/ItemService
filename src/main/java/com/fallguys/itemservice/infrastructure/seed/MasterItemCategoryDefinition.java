package com.fallguys.itemservice.infrastructure.seed;

import com.fallguys.itemservice.domain.ItemCategory;

record MasterItemCategoryDefinition(
        String code,
        String name,
        String parentCode,
        int depth,
        int displayOrder
) {

    String finalCategoryCode() {
        return code;
    }

    ItemCategory toDomain() {
        if (depth == ItemCategory.ROOT_DEPTH) {
            return ItemCategory.root(code, name, displayOrder, true);
        }
        return ItemCategory.subCategory(code, name, parentCode, displayOrder, true);
    }
}
