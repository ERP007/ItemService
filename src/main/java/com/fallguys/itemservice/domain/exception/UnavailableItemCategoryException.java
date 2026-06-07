package com.fallguys.itemservice.domain.exception;

public class UnavailableItemCategoryException extends BusinessException {

    public UnavailableItemCategoryException(String categoryCode) {
        super(ItemErrorCode.UNAVAILABLE_ITEM_CATEGORY, "Item category is unavailable: " + categoryCode);
    }
}
