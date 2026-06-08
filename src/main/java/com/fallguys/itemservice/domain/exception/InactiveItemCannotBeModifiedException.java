package com.fallguys.itemservice.domain.exception;

public class InactiveItemCannotBeModifiedException extends BusinessException {

    public InactiveItemCannotBeModifiedException(String sku) {
        super(ItemErrorCode.INACTIVE_ITEM_CANNOT_BE_MODIFIED, "Inactive item cannot be modified: " + sku);
    }
}
