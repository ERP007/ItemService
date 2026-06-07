package com.fallguys.itemservice.domain.exception;

public class InvalidItemCategoryException extends BusinessException {

    public InvalidItemCategoryException(String message) {
        super(ItemErrorCode.INVALID_ITEM_CATEGORY, message);
    }
}
