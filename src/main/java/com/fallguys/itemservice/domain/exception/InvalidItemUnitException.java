package com.fallguys.itemservice.domain.exception;

public class InvalidItemUnitException extends BusinessException {

    public InvalidItemUnitException(String message) {
        super(ItemErrorCode.INVALID_UNIT, message);
    }
}
