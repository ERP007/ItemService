package com.fallguys.itemservice.infrastructure.seed;

public class MasterItemSeedException extends RuntimeException {

    public MasterItemSeedException(String message) {
        super(message);
    }

    public MasterItemSeedException(String message, Throwable cause) {
        super(message, cause);
    }
}
