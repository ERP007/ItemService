package com.fallguys.itemservice.domain.exception;

public class InvalidItemRequestException extends BusinessException {

    public InvalidItemRequestException(ItemErrorCode errorCode, String message) {
        super(errorCode, message);
    }
}
