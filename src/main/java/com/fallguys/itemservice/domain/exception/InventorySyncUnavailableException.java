package com.fallguys.itemservice.domain.exception;

public class InventorySyncUnavailableException extends BusinessException {

    public InventorySyncUnavailableException(String message, Throwable cause) {
        super(ItemErrorCode.INVENTORY_SYNC_UNAVAILABLE, message, cause);
    }
}
