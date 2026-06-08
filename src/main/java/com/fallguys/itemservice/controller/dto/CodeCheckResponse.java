package com.fallguys.itemservice.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "SKU 중복 확인 응답")
public record CodeCheckResponse(
        @Schema(description = "검증한 부품 SKU", example = "HMC-EN-00214")
        String sku,
        @Schema(description = "사용 가능 여부", example = "false")
        boolean available,
        @Schema(description = "사용 가능 여부 메시지", example = "이미 사용 중인 부품코드입니다.")
        String message
) {

    public static CodeCheckResponse from(String sku, boolean available) {
        String message = available ? "사용 가능한 부품코드입니다." : "이미 사용 중인 부품코드입니다.";
        return new CodeCheckResponse(sku, available, message);
    }
}
