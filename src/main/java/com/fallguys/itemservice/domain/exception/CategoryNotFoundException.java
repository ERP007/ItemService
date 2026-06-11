package com.fallguys.itemservice.domain.exception;

public class CategoryNotFoundException extends BusinessException {

    public CategoryNotFoundException(String categoryCode) {
        super(ItemErrorCode.CATEGORY_NOT_FOUND, "카테고리를 찾을 수 없습니다: " + categoryCode);
    }
}
