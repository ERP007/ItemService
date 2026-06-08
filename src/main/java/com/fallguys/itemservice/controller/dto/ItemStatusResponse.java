package com.fallguys.itemservice.controller.dto;

import com.fallguys.itemservice.domain.Item;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "부품 활성 상태 변경 응답")
public record ItemStatusResponse(
        @Schema(description = "부품 SKU", example = "HMC-WP-00229")
        String sku,
        @Schema(description = "부품명", example = "워터 펌프 어셈블리 (구형)")
        String name,
        @Schema(description = "활성 여부", example = "true")
        boolean active,
        @Schema(description = "최근 수정일시", example = "2026-06-06T10:25:00")
        String updatedAt
) {

    public static ItemStatusResponse from(Item item) {
        return new ItemStatusResponse(
                item.getSku(),
                item.getName(),
                item.isActive(),
                TimeResponseFormatter.format(item.getUpdatedAt())
        );
    }
}
