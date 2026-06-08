package com.fallguys.itemservice.domain.exception;

public class UnavailableItemCategoryException extends BusinessException {

    public UnavailableItemCategoryException(String categoryCode) {
        super(ItemErrorCode.CATEGORY_NOT_FOUND, "Item category is unavailable: " + categoryCode);
    }
}
