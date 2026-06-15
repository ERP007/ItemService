package com.fallguys.itemservice.domain.exception;

public class InactiveItemCannotBeModifiedException extends BusinessException {

    public InactiveItemCannotBeModifiedException(String sku) {
        super(ItemErrorCode.INACTIVE_ITEM_CANNOT_BE_MODIFIED, "비활성 부품은 수정할 수 없습니다: " + sku);
    }
}
