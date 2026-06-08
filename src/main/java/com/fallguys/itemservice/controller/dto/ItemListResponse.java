package com.fallguys.itemservice.controller.dto;

import com.fallguys.itemservice.domain.ItemView;
import com.fallguys.itemservice.domain.PageResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "부품 목록 조회 응답")
public record ItemListResponse(
        @Schema(description = "부품 목록")
        List<ItemListRowResponse> content,
        @Schema(description = "1부터 시작하는 현재 페이지", example = "1")
        int page,
        @Schema(description = "페이지당 개수", example = "10")
        int size,
        @Schema(description = "전체 부품 수", example = "12438")
        long totalElements,
        @Schema(description = "전체 페이지 수", example = "1244")
        int totalPages,
        @Schema(description = "이전 페이지 존재 여부", example = "false")
        boolean hasPrevious,
        @Schema(description = "다음 페이지 존재 여부", example = "true")
        boolean hasNext
) {

    public static ItemListResponse from(PageResult<ItemView> pageResult) {
        return new ItemListResponse(
                pageResult.content().stream()
                        .map(ItemListRowResponse::from)
                        .toList(),
                pageResult.page() + 1,
                pageResult.size(),
                pageResult.totalElements(),
                pageResult.totalPages(),
                pageResult.hasPrevious(),
                pageResult.hasNext()
        );
    }
}
