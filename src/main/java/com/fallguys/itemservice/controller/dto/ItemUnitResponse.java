package com.fallguys.itemservice.controller.dto;

import com.fallguys.itemservice.domain.ItemUnit;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "부품 단위 응답")
public record ItemUnitResponse(
        @Schema(description = "단위 코드", example = "EA")
        String unit,
        @Schema(description = "단위 표시명", example = "EA")
        String name
) {

    public static ItemUnitResponse from(ItemUnit unit) {
        return new ItemUnitResponse(unit.getCode(), unit.getLabel());
    }
}
