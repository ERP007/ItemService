package com.fallguys.itemservice.controller.dto;

import com.fallguys.itemservice.domain.UpdateItemSelectionCommand;
import com.fallguys.itemservice.domain.exception.ItemErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "부품 기본 정보 수정 요청")
public record UpdateItemRequest(
        @Schema(description = "부품명", example = "엔진오일 필터 (2.0L gasoline)")
        String name,
        @Schema(description = "대분류 코드", example = "ENGINE")
        String categoryCode,
        @Schema(description = "중분류 코드", example = "ENGINE_LUBRICATION")
        String subCategoryCode,
        @Schema(description = "단위 코드", allowableValues = {"EA", "BOX", "SET", "L"}, example = "EA")
        String unit,
        @Schema(description = "기준 단가", example = "15000")
        Integer unitPrice,
        @Schema(description = "안전재고 기준", example = "120")
        Integer safetyStock
) {

    public UpdateItemSelectionCommand toCommand(String sku) {
        return new UpdateItemSelectionCommand(
                ItemRequestValidator.requireSku(sku, ItemErrorCode.INVALID_SKU_FORMAT),
                ItemRequestValidator.requireNameForUpdate(name),
                ItemRequestValidator.requireCategoryForUpdate(categoryCode),
                ItemRequestValidator.requireSubCategoryForUpdate(subCategoryCode),
                ItemRequestValidator.requireUnit(unit),
                ItemRequestValidator.requireSafetyStock(safetyStock),
                ItemRequestValidator.requireUnitPrice(unitPrice)
        );
    }
}
