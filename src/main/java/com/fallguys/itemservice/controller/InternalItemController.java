package com.fallguys.itemservice.controller;

import com.fallguys.itemservice.controller.dto.InternalItemCategoryResponse;
import com.fallguys.itemservice.controller.dto.InternalItemDetailResponse;
import com.fallguys.itemservice.controller.dto.ItemBatchRequest;
import com.fallguys.itemservice.controller.dto.ItemBatchResponse;
import com.fallguys.itemservice.controller.dto.ItemRequestValidator;
import com.fallguys.itemservice.domain.Item;
import com.fallguys.itemservice.domain.ItemService;
import com.fallguys.itemservice.domain.ItemView;
import com.fallguys.itemservice.domain.exception.InvalidItemRequestException;
import com.fallguys.itemservice.domain.exception.ItemErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/internal/items")
@RestController
@Tag(name = "Internal Items", description = "내부 서비스 간 부품 마스터 조회 API")
public class InternalItemController {

    private final ItemService itemService;

    public InternalItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping("/{sku}")
    @Operation(
            summary = "내부 부품 조회",
            description = "procurement-service, inventory-service 등 내부 서비스가 SKU 기준으로 부품 마스터 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내부 부품 조회 성공",
                    content = @Content(schema = @Schema(implementation = InternalItemDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "SKU 형식 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "내부 인증 실패",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "내부 API 접근 권한 없음",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 부품",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public InternalItemDetailResponse getInternalItem(
            @Parameter(description = "조회 대상 부품 SKU", example = "HMC-EN-00214")
            @PathVariable String sku
    ) {
        String normalizedSku = ItemRequestValidator.requireSku(sku, ItemErrorCode.INVALID_SKU_FORMAT);
        ItemView item = itemService.getViewBySku(normalizedSku);

        return InternalItemDetailResponse.from(item);
    }

    @GetMapping("/category/{sku}")
    @Operation(
            summary = "내부 부품 분류 조회",
            description = "inventory-service 등 내부 서비스가 SKU 기준으로 부품의 대분류, 중분류 정보를 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내부 부품 분류 조회 성공",
                    content = @Content(schema = @Schema(implementation = InternalItemCategoryResponse.class))),
            @ApiResponse(responseCode = "400", description = "SKU 형식 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "내부 인증 실패",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "내부 API 접근 권한 없음",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 부품",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public InternalItemCategoryResponse getInternalItemCategory(
            @Parameter(description = "조회 대상 부품 SKU", example = "HMC-EN-00214")
            @PathVariable String sku
    ) {
        String normalizedSku = ItemRequestValidator.requireSku(sku, ItemErrorCode.INVALID_SKU_FORMAT);
        ItemView item = itemService.getViewBySku(normalizedSku);

        return InternalItemCategoryResponse.from(item);
    }

    @PostMapping("/batch")
    @Operation(
            summary = "내부 부품 배치 조회",
            description = "발주 라인 렌더링, 재고 처리 등에서 여러 SKU의 부품 마스터 정보를 한 번에 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "내부 부품 배치 조회 성공",
                    content = @Content(schema = @Schema(implementation = ItemBatchResponse.class))),
            @ApiResponse(responseCode = "400", description = "skus 누락, SKU 형식 오류, 조회 개수 초과",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "내부 인증 실패",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "내부 API 접근 권한 없음",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ItemBatchResponse getInternalItems(
            @RequestBody(required = false) ItemBatchRequest request
    ) {
        if (request == null) {
            throw new InvalidItemRequestException(ItemErrorCode.SKUS_REQUIRED, "SKU 목록은 필수입니다.");
        }
        List<String> normalizedSkus = request.normalizedSkus();
        List<Item> foundItems = itemService.getBySkus(normalizedSkus);

        return ItemBatchResponse.from(normalizedSkus, foundItems);
    }
}
