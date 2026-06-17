package com.fallguys.itemservice.domain.exception;

public class InventorySyncFailedException extends BusinessException {

    public InventorySyncFailedException(String message, Throwable cause) {
        super(ItemErrorCode.INVENTORY_SYNC_FAILED, message, cause);
    }
}
