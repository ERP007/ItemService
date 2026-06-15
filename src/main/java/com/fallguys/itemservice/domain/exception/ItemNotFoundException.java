package com.fallguys.itemservice.domain.exception;

public class ItemNotFoundException extends BusinessException {

    public ItemNotFoundException(String sku) {
        super(ItemErrorCode.ITEM_NOT_FOUND, "부품을 찾을 수 없습니다: " + sku);
    }
}
