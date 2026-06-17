package com.fallguys.itemservice.controller.dto;

import com.fallguys.itemservice.domain.ItemView;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "부품 목록 행 응답")
public record ItemListRowResponse(
        @Schema(description = "부품 SKU", example = "HMC-EN-00214")
        String sku,
        @Schema(description = "부품명", example = "엔진오일 필터 (2.0L gasoline)")
        String name,
        @Schema(description = "중분류 코드", example = "ENGINE_LUBRICATION")
        String categoryCode,
        @Schema(description = "중분류명", example = "윤활계통")
        String categoryName,
        @Schema(description = "대분류 코드", example = "ENGINE")
        String parentCategoryCode,
        @Schema(description = "대분류명", example = "엔진")
        String parentCategoryName,
        @Schema(description = "단위 코드", example = "EA")
        String unit,
        @Schema(description = "안전재고 기준", example = "120")
        int safetyStock,
        @Schema(description = "기준 단가", example = "15000")
        int unitPrice,
        @Schema(description = "활성 여부", example = "true")
        boolean active,
        @Schema(description = "등록일시", example = "2023-04-12T09:00:00")
        String createdAt,
        @Schema(description = "최근 수정일시", example = "2025-11-02T14:30:00")
        String updatedAt
) {

    public static ItemListRowResponse from(ItemView item) {
        return new ItemListRowResponse(
                item.sku(),
                item.name(),
                ItemCategoryHierarchyFields.subCategoryCode(item),
                ItemCategoryHierarchyFields.subCategoryName(item),
                ItemCategoryHierarchyFields.majorCategoryCode(item),
                ItemCategoryHierarchyFields.majorCategoryName(item),
                item.unit().getCode(),
                item.safetyStock(),
                item.unitPrice(),
                item.active(),
                TimeResponseFormatter.format(item.createdAt()),
                TimeResponseFormatter.format(item.updatedAt())
        );
    }
}
