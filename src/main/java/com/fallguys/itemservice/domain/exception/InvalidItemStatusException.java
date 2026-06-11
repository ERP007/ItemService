package com.fallguys.itemservice.domain.exception;

public class InvalidItemStatusException extends BusinessException {

    private InvalidItemStatusException(String message) {
        super(ItemErrorCode.INVALID_ITEM_STATUS, message);
    }

    public static InvalidItemStatusException alreadyActive(String sku) {
        return new InvalidItemStatusException("이미 활성 상태인 부품입니다: " + sku);
    }

    public static InvalidItemStatusException alreadyInactive(String sku) {
        return new InvalidItemStatusException("이미 비활성 상태인 부품입니다: " + sku);
    }
}
