package com.fallguys.itemservice.controller.dto;

import com.fallguys.itemservice.domain.ItemView;

final class ItemCategoryHierarchyFields {

    private ItemCategoryHierarchyFields() {
    }

    static String majorCategoryCode(ItemView item) {
        if (isRootCategory(item)) {
            return item.categoryCode();
        }
        return item.parentCategoryCode();
    }

    static String majorCategoryName(ItemView item) {
        if (isRootCategory(item)) {
            return item.categoryName();
        }
        return item.parentCategoryName();
    }

    static String subCategoryCode(ItemView item) {
        if (isRootCategory(item)) {
            return null;
        }
        return item.categoryCode();
    }

    static String subCategoryName(ItemView item) {
        if (isRootCategory(item)) {
            return null;
        }
        return item.categoryName();
    }

    private static boolean isRootCategory(ItemView item) {
        return item.parentCategoryCode() == null;
    }
}
