package com.fallguys.itemservice.controller.dto;

import com.fallguys.itemservice.domain.exception.ItemErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "SKU 중복 확인 요청")
public record CodeCheckRequest(
        @Schema(description = "검증할 부품 SKU", example = "HMC-EN-00214")
        String sku
) {

    public String normalizedSku() {
        return ItemRequestValidator.requireSku(sku, ItemErrorCode.SKU_REQUIRED);
    }
}
