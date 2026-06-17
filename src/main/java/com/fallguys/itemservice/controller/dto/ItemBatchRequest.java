package com.fallguys.itemservice.controller.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "부품 배치 조회 요청")
public record ItemBatchRequest(
        @Schema(description = "조회할 부품 SKU 목록", example = "[\"HMC-EN-00214\", \"HMC-NO-99999\"]")
        List<String> skus
) {

    private static final int MAX_SKUS = 100;

    public List<String> normalizedSkus() {
        return ItemRequestValidator.requireSkus(skus, MAX_SKUS);
    }
}
