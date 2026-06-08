package com.fallguys.itemservice.domain.exception;

public class InvalidItemStatusException extends BusinessException {

    private InvalidItemStatusException(String message) {
        super(ItemErrorCode.INVALID_ITEM_STATUS, message);
    }

    public static InvalidItemStatusException alreadyActive(String sku) {
        return new InvalidItemStatusException("Item is already active: " + sku);
    }

    public static InvalidItemStatusException alreadyInactive(String sku) {
        return new InvalidItemStatusException("Item is already inactive: " + sku);
    }
}
