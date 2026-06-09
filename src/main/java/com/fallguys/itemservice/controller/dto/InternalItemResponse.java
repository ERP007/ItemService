package com.fallguys.itemservice.controller.dto;

import com.fallguys.itemservice.domain.Item;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내부 서비스용 부품 조회 응답")
public record InternalItemResponse(
        @Schema(description = "부품 SKU", example = "HMC-EN-00214")
        String sku,
        @Schema(description = "부품명", example = "엔진오일 필터 (2.0L gasoline)")
        String name,
        @Schema(description = "분류 코드", example = "ENGINE_LUBRICATION")
        String categoryCode,
        @Schema(description = "단위 코드", example = "EA")
        String unit,
        @Schema(description = "기준 단가", example = "15000")
        int unitPrice,
        @Schema(description = "안전재고 기준", example = "120")
        int safetyStock,
        @Schema(description = "활성 여부", example = "true")
        boolean active
) {

    public static InternalItemResponse from(Item item) {
        return new InternalItemResponse(
                item.getSku(),
                item.getName(),
                item.getCategoryCode(),
                item.getUnit().getCode(),
                item.getUnitPrice(),
                item.getSafetyStock(),
                item.isActive()
        );
    }
}
