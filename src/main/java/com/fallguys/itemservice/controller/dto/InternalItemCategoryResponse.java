package com.fallguys.itemservice.controller.dto;

import com.fallguys.itemservice.domain.ItemView;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "내부 서비스용 부품 분류 조회 응답")
public record InternalItemCategoryResponse(
        @Schema(description = "부품 SKU", example = "HMC-EN-00214")
        String sku,
        @Schema(description = "부품 대분류", example = "엔진")
        String majorCategory,
        @Schema(description = "부품 중분류", example = "필터")
        String middleCategory
) {

    public static InternalItemCategoryResponse from(ItemView item) {
        return new InternalItemCategoryResponse(
                item.sku(),
                majorCategory(item),
                middleCategory(item)
        );
    }

    static String majorCategory(ItemView item) {
        if (item.parentCategoryCode() == null) {
            return item.categoryName();
        }
        return item.parentCategoryName();
    }

    static String middleCategory(ItemView item) {
        if (item.parentCategoryCode() == null) {
            return "";
        }
        return item.categoryName();
    }
}
