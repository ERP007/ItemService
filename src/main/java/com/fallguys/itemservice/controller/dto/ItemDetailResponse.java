package com.fallguys.itemservice.controller.dto;

import com.fallguys.itemservice.domain.ItemView;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "부품 상세 응답")
public record ItemDetailResponse(
        @Schema(description = "부품 SKU", example = "HMC-EN-00214")
        String sku,
        @Schema(description = "부품명", example = "엔진오일 필터 (2.0L gasoline)")
        String name,
        @Schema(description = "대분류 코드", example = "ENGINE")
        String categoryCode,
        @Schema(description = "대분류명", example = "엔진")
        String categoryName,
        @Schema(description = "중분류 코드", example = "ENGINE_LUBRICATION")
        String subCategoryCode,
        @Schema(description = "중분류명", example = "윤활계통")
        String subCategoryName,
        @Schema(description = "단위 코드", example = "EA")
        String unit,
        @Schema(description = "기준 단가", example = "15000")
        int unitPrice,
        @Schema(description = "안전재고 기준", example = "120")
        int safetyStock,
        @Schema(description = "활성 여부", example = "true")
        boolean active,
        @Schema(description = "등록일", example = "2023-04-12")
        String createdAt,
        @Schema(description = "최근 수정일", example = "2026-06-07")
        String updatedAt
) {

    public static ItemDetailResponse from(ItemView item) {
        return new ItemDetailResponse(
                item.sku(),
                item.name(),
                ItemCategoryHierarchyFields.majorCategoryCode(item),
                ItemCategoryHierarchyFields.majorCategoryName(item),
                ItemCategoryHierarchyFields.subCategoryCode(item),
                ItemCategoryHierarchyFields.subCategoryName(item),
                item.unit().getCode(),
                item.unitPrice(),
                item.safetyStock(),
                item.active(),
                TimeResponseFormatter.formatDate(item.createdAt()),
                TimeResponseFormatter.formatDate(item.updatedAt())
        );
    }
}
