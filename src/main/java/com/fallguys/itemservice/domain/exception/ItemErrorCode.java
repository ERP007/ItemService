package com.fallguys.itemservice.domain.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ItemErrorCode implements ErrorCode {
    // 400 BAD_REQUEST
    INVALID_REQUEST("ITM-001", "요청이 유효하지 않습니다."),
    INVALID_CATEGORY_CODE("ITM-002", "카테고리 코드 형식이 올바르지 않습니다."),

    // 404 NOT_FOUND
    CATEGORY_NOT_FOUND("ITM-003", "카테고리를 찾을 수 없습니다."),

    // 400 BAD_REQUEST
    SKU_REQUIRED("ITM-004", "SKU는 필수입니다."),
    SKUS_REQUIRED("ITM-005", "SKU 목록은 필수입니다."),
    INVALID_SKU_FORMAT("ITM-006", "SKU 형식이 올바르지 않습니다."),
    TOO_MANY_SKUS("ITM-007", "조회할 SKU가 너무 많습니다."),
    ITEM_NAME_REQUIRED("ITM-008", "부품명은 필수입니다."),
    CATEGORY_REQUIRED("ITM-009", "카테고리는 필수입니다."),
    INVALID_UNIT("ITM-010", "단위가 올바르지 않습니다."),
    INVALID_SAFETY_STOCK("ITM-011", "안전재고가 올바르지 않습니다."),
    INVALID_UNIT_PRICE("ITM-012", "기준 단가가 올바르지 않습니다."),
    DUPLICATE_SKU("ITM-013", "이미 존재하는 SKU입니다."),
    INVALID_ITEM_NAME("ITM-014", "부품명이 올바르지 않습니다."),
    INVALID_CATEGORY("ITM-015", "대분류가 올바르지 않습니다."),
    INVALID_SUB_CATEGORY("ITM-016", "중분류가 올바르지 않습니다."),
    INVALID_ITEM_STATUS("ITM-017", "부품 상태가 올바르지 않습니다."),
    INACTIVE_ITEM_CANNOT_BE_MODIFIED("ITM-018", "비활성 부품은 수정할 수 없습니다."),

    // 404 NOT_FOUND
    ITEM_NOT_FOUND("ITM-019", "부품을 찾을 수 없습니다."),

    // 409 CONFLICT
    CONCURRENT_MODIFICATION("ITM-020", "다른 사용자가 먼저 수정했습니다."),

    // 500 INTERNAL_SERVER_ERROR
    INTERNAL_ERROR("ITM-021", "서버 내부 오류가 발생했습니다.");

    private final String code;
    private final String defaultMessage;
}
