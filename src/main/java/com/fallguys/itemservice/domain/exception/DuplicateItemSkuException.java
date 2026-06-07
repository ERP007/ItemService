package com.fallguys.itemservice.domain.exception;

public class DuplicateItemSkuException extends BusinessException {

    public DuplicateItemSkuException(String sku) {
        super(ItemErrorCode.DUPLICATE_ITEM_SKU, "Item sku already exists: " + sku);
    }
}
