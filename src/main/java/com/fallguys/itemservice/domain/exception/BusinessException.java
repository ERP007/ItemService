package com.fallguys.itemservice.domain.exception;

import java.util.Objects;

public abstract class BusinessException extends RuntimeException {

    private final ItemErrorCode errorCode;

    protected BusinessException(ItemErrorCode errorCode, String message) {
        super(resolveMessage(errorCode, message));
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
    }

    protected BusinessException(ItemErrorCode errorCode, String message, Throwable cause) {
        super(resolveMessage(errorCode, message), cause);
        this.errorCode = Objects.requireNonNull(errorCode, "errorCode");
    }

    public String getCode() {
        return errorCode.getCode();
    }

    public ItemErrorCode getErrorCode() {
        return errorCode;
    }

    private static String resolveMessage(ItemErrorCode errorCode, String message) {
        ItemErrorCode resolvedErrorCode = Objects.requireNonNull(errorCode, "errorCode");
        if (message == null || message.isBlank()) {
            return resolvedErrorCode.getDefaultMessage();
        }
        return message;
    }
}
