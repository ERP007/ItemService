package com.fallguys.itemservice.domain.exception;

public enum ItemErrorCode {
    UNAUTHORIZED("UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN("FORBIDDEN", "접근 권한이 없습니다."),
    INVALID_PARAMETER("INVALID_PARAMETER", "요청 파라미터가 올바르지 않습니다."),
    INVALID_CATEGORY_CODE("INVALID_CATEGORY_CODE", "카테고리 코드 형식이 올바르지 않습니다."),
    CATEGORY_NOT_FOUND("CATEGORY_NOT_FOUND", "카테고리를 찾을 수 없습니다."),
    SKU_REQUIRED("SKU_REQUIRED", "SKU는 필수입니다."),
    SKUS_REQUIRED("SKUS_REQUIRED", "SKU 목록은 필수입니다."),
    INVALID_SKU_FORMAT("INVALID_SKU_FORMAT", "SKU 형식이 올바르지 않습니다."),
    TOO_MANY_SKUS("TOO_MANY_SKUS", "조회할 SKU가 너무 많습니다."),
    ITEM_NAME_REQUIRED("ITEM_NAME_REQUIRED", "부품명은 필수입니다."),
    CATEGORY_REQUIRED("CATEGORY_REQUIRED", "카테고리는 필수입니다."),
    INVALID_UNIT("INVALID_UNIT", "단위가 올바르지 않습니다."),
    INVALID_SAFETY_STOCK("INVALID_SAFETY_STOCK", "안전재고가 올바르지 않습니다."),
    INVALID_UNIT_PRICE("INVALID_UNIT_PRICE", "기준 단가가 올바르지 않습니다."),
    DUPLICATE_SKU("DUPLICATE_SKU", "이미 존재하는 SKU입니다."),
    INVALID_ITEM_NAME("INVALID_ITEM_NAME", "부품명이 올바르지 않습니다."),
    INVALID_CATEGORY("INVALID_CATEGORY", "대분류가 올바르지 않습니다."),
    INVALID_SUB_CATEGORY("INVALID_SUB_CATEGORY", "중분류가 올바르지 않습니다."),
    INVALID_ITEM_STATUS("INVALID_ITEM_STATUS", "부품 상태가 올바르지 않습니다."),
    INACTIVE_ITEM_CANNOT_BE_MODIFIED("INACTIVE_ITEM_CANNOT_BE_MODIFIED", "비활성 부품은 수정할 수 없습니다."),
    ITEM_NOT_FOUND("ITEM_NOT_FOUND", "부품을 찾을 수 없습니다."),
    CONCURRENT_MODIFICATION("CONCURRENT_MODIFICATION", "다른 사용자가 먼저 수정했습니다."),
    INTERNAL_ERROR("INTERNAL_ERROR", "서버 내부 오류가 발생했습니다.");

    private final String code;
    private final String message;

    ItemErrorCode(String code, String message) {
        this.code = code;
        this.message = message;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
