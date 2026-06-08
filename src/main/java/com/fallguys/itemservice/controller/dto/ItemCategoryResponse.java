package com.fallguys.itemservice.controller.dto;

import com.fallguys.itemservice.domain.ItemCategory;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "부품 대분류 응답")
public record ItemCategoryResponse(
        @Schema(description = "대분류 코드", example = "ENGINE")
        String categoryCode,
        @Schema(description = "대분류명", example = "엔진")
        String categoryName,
        @Schema(description = "표시 순서", example = "1")
        int displayOrder
) {

    public static ItemCategoryResponse from(ItemCategory category) {
        return new ItemCategoryResponse(category.getCode(), category.getName(), category.getDisplayOrder());
    }
}
