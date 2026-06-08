package com.fallguys.itemservice.domain.exception;

public class InvalidItemException extends BusinessException {

    public InvalidItemException(String message) {
        super(ItemErrorCode.INVALID_PARAMETER, message);
    }
}
