package com.fallguys.itemservice.domain.exception;

public enum ItemErrorCode {
    INVALID_ITEM("ITEM-001", "Invalid item."),
    INVALID_ITEM_CATEGORY("ITEM-002", "Invalid item category."),
    INVALID_ITEM_UNIT("ITEM-003", "Invalid item unit.");

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
