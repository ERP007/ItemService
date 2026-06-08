package com.fallguys.itemservice.domain.exception;

public class CategoryNotFoundException extends BusinessException {

    public CategoryNotFoundException(String categoryCode) {
        super(ItemErrorCode.CATEGORY_NOT_FOUND, "Category not found: " + categoryCode);
    }
}
