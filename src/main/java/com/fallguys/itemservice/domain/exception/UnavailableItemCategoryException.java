package com.fallguys.itemservice.domain.exception;

public class UnavailableItemCategoryException extends BusinessException {

    public UnavailableItemCategoryException(String categoryCode) {
        super(ItemErrorCode.CATEGORY_NOT_FOUND, "사용할 수 없는 카테고리입니다: " + categoryCode);
    }
}
