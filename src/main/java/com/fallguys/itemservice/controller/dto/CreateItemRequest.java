package com.fallguys.itemservice.controller.dto;

import com.fallguys.itemservice.domain.CreateItemCommand;
import com.fallguys.itemservice.domain.exception.ItemErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "부품 신규 등록 요청")
public record CreateItemRequest(
        @Schema(description = "사용자가 입력한 부품 SKU", example = "HMC-EN-00214")
        String sku,
        @Schema(description = "부품명", example = "엔진오일 필터 (2.0L gasoline)")
        String name,
        @Schema(description = "최종 선택된 중분류 코드", example = "ENGINE_LUBRICATION")
        String categoryCode,
        @Schema(description = "단위 코드", allowableValues = {"EA", "BOX", "SET", "L"}, example = "EA")
        String unit,
        @Schema(description = "안전재고 기준", example = "120")
        Integer safetyStock,
        @Schema(description = "기준 단가", example = "15000")
        Integer unitPrice
) {

    public CreateItemCommand toCommand() {
        return new CreateItemCommand(
                ItemRequestValidator.requireSku(sku, ItemErrorCode.SKU_REQUIRED),
                ItemRequestValidator.requireNameForCreate(name),
                ItemRequestValidator.requireCategoryForCreate(categoryCode),
                ItemRequestValidator.requireUnit(unit),
                ItemRequestValidator.requireSafetyStock(safetyStock),
                ItemRequestValidator.requireUnitPrice(unitPrice)
        );
    }
}
