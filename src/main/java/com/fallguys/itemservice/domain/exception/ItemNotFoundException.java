package com.fallguys.itemservice.domain.exception;

public class ItemNotFoundException extends BusinessException {

    public ItemNotFoundException(String sku) {
        super(ItemErrorCode.ITEM_NOT_FOUND, "Item not found: " + sku);
    }
}
