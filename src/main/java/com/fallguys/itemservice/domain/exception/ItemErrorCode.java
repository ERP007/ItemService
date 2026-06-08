package com.fallguys.itemservice.domain.exception;

public enum ItemErrorCode {
    INVALID_PARAMETER("INVALID_PARAMETER", "Invalid parameter."),
    INVALID_CATEGORY_CODE("INVALID_CATEGORY_CODE", "Invalid category code."),
    CATEGORY_NOT_FOUND("CATEGORY_NOT_FOUND", "Category not found."),
    SKU_REQUIRED("SKU_REQUIRED", "SKU is required."),
    INVALID_SKU_FORMAT("INVALID_SKU_FORMAT", "Invalid SKU format."),
    ITEM_NAME_REQUIRED("ITEM_NAME_REQUIRED", "Item name is required."),
    CATEGORY_REQUIRED("CATEGORY_REQUIRED", "Category is required."),
    INVALID_UNIT("INVALID_UNIT", "Invalid unit."),
    INVALID_SAFETY_STOCK("INVALID_SAFETY_STOCK", "Invalid safety stock."),
    INVALID_UNIT_PRICE("INVALID_UNIT_PRICE", "Invalid unit price."),
    DUPLICATE_SKU("DUPLICATE_SKU", "Duplicate SKU."),
    INVALID_ITEM_NAME("INVALID_ITEM_NAME", "Invalid item name."),
    INVALID_CATEGORY("INVALID_CATEGORY", "Invalid category."),
    INVALID_SUB_CATEGORY("INVALID_SUB_CATEGORY", "Invalid sub category."),
    INACTIVE_ITEM_CANNOT_BE_MODIFIED("INACTIVE_ITEM_CANNOT_BE_MODIFIED", "Inactive item cannot be modified."),
    ITEM_NOT_FOUND("ITEM_NOT_FOUND", "Item not found."),
    INTERNAL_ERROR("INTERNAL_ERROR", "Internal error.");

    private final String code;
    private final String message;

    ItemErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
