package com.fallguys.itemservice.domain.exception;

public class DuplicateItemSkuException extends BusinessException {

    public DuplicateItemSkuException(String sku) {
        super(ItemErrorCode.DUPLICATE_SKU, "이미 존재하는 SKU입니다: " + sku);
    }
}
