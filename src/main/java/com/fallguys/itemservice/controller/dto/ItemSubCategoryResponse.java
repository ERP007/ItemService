package com.fallguys.itemservice.controller.dto;

import com.fallguys.itemservice.domain.ItemCategory;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "부품 중분류 응답")
public record ItemSubCategoryResponse(
        @Schema(description = "중분류 코드", example = "ENGINE_LUBRICATION")
        String categoryCode,
        @Schema(description = "중분류명", example = "윤활계통")
        String categoryName,
        @Schema(description = "상위 대분류 코드", example = "ENGINE")
        String parentCategoryCode,
        @Schema(description = "표시 순서", example = "1")
        int displayOrder
) {

    public static ItemSubCategoryResponse from(ItemCategory category) {
        return new ItemSubCategoryResponse(
                category.getCode(),
                category.getName(),
                category.getParentCode(),
                category.getDisplayOrder()
        );
    }
}
