package com.fallguys.itemservice.controller.dto;

import com.fallguys.itemservice.domain.Item;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Schema(description = "부품 배치 조회 응답")
public record ItemBatchResponse(
        @Schema(description = "조회된 부품 목록")
        List<ItemBatchItemResponse> items,
        @Schema(description = "존재하지 않는 SKU 목록", example = "[\"HMC-NO-99999\"]")
        List<String> notFoundSkus
) {

    public static ItemBatchResponse from(List<String> requestedSkus, List<Item> foundItems) {
        Map<String, Item> itemBySku = foundItems.stream()
                .collect(Collectors.toMap(Item::getSku, Function.identity()));
        List<ItemBatchItemResponse> items = requestedSkus.stream()
                .filter(itemBySku::containsKey)
                .map(itemBySku::get)
                .map(ItemBatchItemResponse::from)
                .toList();
        List<String> notFoundSkus = requestedSkus.stream()
                .filter(sku -> !itemBySku.containsKey(sku))
                .toList();

        return new ItemBatchResponse(items, notFoundSkus);
    }
}
