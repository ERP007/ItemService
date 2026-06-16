package com.fallguys.itemservice.domain.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

class ItemErrorCodeTest {

    @Test
    void matchesPublicErrorCodeSpecification() {
        assertAll(
                () -> assertEquals("ITM-001", ItemErrorCode.INVALID_REQUEST.getCode()),
                () -> assertEquals("ITM-002", ItemErrorCode.INVALID_CATEGORY_CODE.getCode()),
                () -> assertEquals("ITM-003", ItemErrorCode.CATEGORY_NOT_FOUND.getCode()),
                () -> assertEquals("ITM-004", ItemErrorCode.SKU_REQUIRED.getCode()),
                () -> assertEquals("ITM-005", ItemErrorCode.SKUS_REQUIRED.getCode()),
                () -> assertEquals("ITM-006", ItemErrorCode.INVALID_SKU_FORMAT.getCode()),
                () -> assertEquals("ITM-007", ItemErrorCode.TOO_MANY_SKUS.getCode()),
                () -> assertEquals("ITM-008", ItemErrorCode.ITEM_NAME_REQUIRED.getCode()),
                () -> assertEquals("ITM-009", ItemErrorCode.CATEGORY_REQUIRED.getCode()),
                () -> assertEquals("ITM-010", ItemErrorCode.INVALID_UNIT.getCode()),
                () -> assertEquals("ITM-011", ItemErrorCode.INVALID_SAFETY_STOCK.getCode()),
                () -> assertEquals("ITM-012", ItemErrorCode.INVALID_UNIT_PRICE.getCode()),
                () -> assertEquals("ITM-013", ItemErrorCode.DUPLICATE_SKU.getCode()),
                () -> assertEquals("ITM-014", ItemErrorCode.INVALID_ITEM_NAME.getCode()),
                () -> assertEquals("ITM-015", ItemErrorCode.INVALID_CATEGORY.getCode()),
                () -> assertEquals("ITM-016", ItemErrorCode.INVALID_SUB_CATEGORY.getCode()),
                () -> assertEquals("ITM-017", ItemErrorCode.INVALID_ITEM_STATUS.getCode()),
                () -> assertEquals("ITM-018", ItemErrorCode.INACTIVE_ITEM_CANNOT_BE_MODIFIED.getCode()),
                () -> assertEquals("ITM-019", ItemErrorCode.ITEM_NOT_FOUND.getCode()),
                () -> assertEquals("ITM-020", ItemErrorCode.CONCURRENT_MODIFICATION.getCode()),
                () -> assertEquals("ITM-021", ItemErrorCode.INTERNAL_ERROR.getCode()),
                () -> assertEquals("ITM-022", ItemErrorCode.INVENTORY_SYNC_FAILED.getCode()),
                () -> assertEquals("ITM-023", ItemErrorCode.INVENTORY_SYNC_UNAVAILABLE.getCode())
        );
    }
}
