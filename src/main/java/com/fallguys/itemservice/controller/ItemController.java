package com.fallguys.itemservice.controller;

import com.fallguys.itemservice.controller.dto.CodeCheckRequest;
import com.fallguys.itemservice.controller.dto.CodeCheckResponse;
import com.fallguys.itemservice.controller.dto.CreateItemRequest;
import com.fallguys.itemservice.controller.dto.CreateItemResponse;
import com.fallguys.itemservice.controller.dto.ItemDetailResponse;
import com.fallguys.itemservice.controller.dto.ItemListResponse;
import com.fallguys.itemservice.controller.dto.ItemRequestValidator;
import com.fallguys.itemservice.controller.dto.ItemStatusResponse;
import com.fallguys.itemservice.controller.dto.ItemUnitResponse;
import com.fallguys.itemservice.controller.dto.UpdateItemRequest;
import com.fallguys.itemservice.controller.dto.UpdateItemResponse;
import com.fallguys.itemservice.domain.CreateItemCommand;
import com.fallguys.itemservice.domain.Item;
import com.fallguys.itemservice.domain.ItemService;
import com.fallguys.itemservice.domain.ItemSortBy;
import com.fallguys.itemservice.domain.ItemView;
import com.fallguys.itemservice.domain.PageResult;
import com.fallguys.itemservice.domain.SearchItemsQuery;
import com.fallguys.itemservice.domain.SortDirection;
import com.fallguys.itemservice.domain.UpdateItemSelectionCommand;
import com.fallguys.itemservice.domain.exception.InvalidItemRequestException;
import com.fallguys.itemservice.domain.exception.ItemErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping({"/api/items", "/items"})
@RestController
@Tag(name = "Items", description = "부품 마스터 목록, 등록, 수정, SKU 중복 확인 API")
public class ItemController {

    private static final int DEFAULT_PAGE = 1;
    private static final int DEFAULT_SIZE = 10;
    private static final String DEFAULT_STATUS = "ALL";
    private static final String DEFAULT_SORT = "updatedAt,desc";

    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping
    @Operation(
            summary = "부품 목록 조회",
            description = "부품명/SKU 검색, 대분류/중분류 필터, 상태 필터, 정렬, 페이지네이션 조건으로 부품 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "부품 목록 조회 성공",
                    content = @Content(schema = @Schema(implementation = ItemListResponse.class))),
            @ApiResponse(responseCode = "400", description = "status, sort, page, size, categoryCode 값 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 categoryCode",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ItemListResponse search(
            @Parameter(description = "SKU 또는 부품명 부분 일치", example = "oil")
            @RequestParam(required = false) String search,
            @Parameter(description = "대분류 또는 중분류 category code", example = "ENGINE_LUBRICATION")
            @RequestParam(required = false) String categoryCode,
            @Parameter(description = "ACTIVE, INACTIVE, ALL", example = "ALL")
            @RequestParam(defaultValue = DEFAULT_STATUS) String status,
            @Parameter(description = "1부터 시작하는 페이지 번호", example = "1")
            @RequestParam(defaultValue = "" + DEFAULT_PAGE) int page,
            @Parameter(description = "페이지당 개수", example = "10")
            @RequestParam(defaultValue = "" + DEFAULT_SIZE) int size,
            @Parameter(description = "정렬 필드와 방향. 허용 필드: sku, name, createdAt, updatedAt, safetyStock", example = "updatedAt,desc")
            @RequestParam(defaultValue = DEFAULT_SORT) String sort
    ) {
        SearchItemsQuery query = new SearchItemsQuery(
                ItemRequestValidator.trimToNull(search),
                normalizeCategoryCode(categoryCode),
                parseStatus(status),
                toZeroBasedPage(page),
                requireSize(size),
                parseSortBy(sort),
                parseSortDirection(sort)
        );
        PageResult<ItemView> result = itemService.searchViews(query);

        return ItemListResponse.from(result);
    }

    @GetMapping("/{sku}")
    @Operation(
            summary = "부품 상세 조회",
            description = "IM-03 부품 상세 화면에서 SKU 기준으로 부품 마스터 상세 정보를 조회합니다. 재고 정보는 Inventory API에서 별도 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "부품 상세 조회 성공",
                    content = @Content(schema = @Schema(implementation = ItemDetailResponse.class))),
            @ApiResponse(responseCode = "400", description = "SKU 형식 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "미인증 사용자",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "부품 상세 조회 권한 없음",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 부품",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ItemDetailResponse getDetail(
            @Parameter(description = "조회 대상 부품 SKU", example = "HMC-EN-00214")
            @PathVariable String sku
    ) {
        String normalizedSku = ItemRequestValidator.requireSku(sku, ItemErrorCode.INVALID_SKU_FORMAT);
        ItemView item = itemService.getViewBySku(normalizedSku);

        return ItemDetailResponse.from(item);
    }

    @PostMapping
    @Operation(
            summary = "부품 신규 등록",
            description = "사용자가 입력한 SKU와 부품 기본 정보로 신규 부품을 활성 상태로 등록합니다. ADMIN, HQ_MANAGER, HQ_STAFF 권한만 허용합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "201", description = "부품 등록 성공",
                    content = @Content(schema = @Schema(implementation = CreateItemResponse.class))),
            @ApiResponse(responseCode = "400", description = "SKU, 부품명, 분류, 단위, 안전재고, 단가 검증 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 중분류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "이미 존재하는 SKU",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "미인증 사용자",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "부품 등록 권한 없음",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<CreateItemResponse> create(@Valid @RequestBody CreateItemRequest request) {
        CreateItemCommand command = request.toCommand();
        ItemView created = itemService.createView(command);

        return ResponseEntity.status(HttpStatus.CREATED).body(CreateItemResponse.from(created));
    }

    @PatchMapping("/{sku}")
    @Operation(
            summary = "부품 기본 정보 수정",
            description = "SKU를 제외한 부품명, 대분류/중분류, 단위, 기준 단가, 안전재고 기준을 수정합니다. ADMIN, HQ_MANAGER, HQ_STAFF 권한만 허용합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "부품 수정 성공",
                    content = @Content(schema = @Schema(implementation = UpdateItemResponse.class))),
            @ApiResponse(responseCode = "400", description = "요청값 검증 오류 또는 비활성 부품 수정 시도",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "미인증 사용자",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "부품 수정 권한 없음",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 부품",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public UpdateItemResponse update(
            @Parameter(description = "수정 대상 부품 SKU", example = "HMC-EN-00214")
            @PathVariable String sku,
            @Valid @RequestBody UpdateItemRequest request
    ) {
        UpdateItemSelectionCommand command = request.toCommand(sku);

        return UpdateItemResponse.from(itemService.updateSelection(command));
    }

    @PatchMapping("/{sku}/activate")
    @Operation(
            summary = "부품 활성 복귀",
            description = "비활성 상태의 부품을 활성 상태로 복귀합니다. ADMIN, HQ_MANAGER, HQ_STAFF 권한은 Gateway/Auth에서 검증합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "부품 활성 복귀 성공",
                    content = @Content(schema = @Schema(implementation = ItemStatusResponse.class))),
            @ApiResponse(responseCode = "400", description = "SKU 형식 오류 또는 이미 활성 상태",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "미인증 사용자",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "활성 복귀 권한 없음",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 부품",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "다른 사용자가 먼저 상태 변경",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ItemStatusResponse activate(
            @Parameter(description = "활성 복귀 대상 부품 SKU", example = "HMC-WP-00229")
            @PathVariable String sku
    ) {
        String normalizedSku = ItemRequestValidator.requireSku(sku, ItemErrorCode.INVALID_SKU_FORMAT);
        Item item = itemService.activate(normalizedSku);

        return ItemStatusResponse.from(item);
    }

    @PatchMapping("/{sku}/deactivate")
    @Operation(
            summary = "부품 비활성 전환",
            description = "활성 상태의 부품을 비활성 상태로 전환합니다. ADMIN, HQ_MANAGER, HQ_STAFF 권한은 Gateway/Auth에서 검증합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "부품 비활성 전환 성공",
                    content = @Content(schema = @Schema(implementation = ItemStatusResponse.class))),
            @ApiResponse(responseCode = "400", description = "SKU 형식 오류 또는 이미 비활성 상태",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "미인증 사용자",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "비활성 전환 권한 없음",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 부품",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "409", description = "다른 사용자가 먼저 상태 변경",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ItemStatusResponse deactivate(
            @Parameter(description = "비활성 전환 대상 부품 SKU", example = "HMC-WP-00229")
            @PathVariable String sku
    ) {
        String normalizedSku = ItemRequestValidator.requireSku(sku, ItemErrorCode.INVALID_SKU_FORMAT);
        Item item = itemService.deactivate(normalizedSku);

        return ItemStatusResponse.from(item);
    }

    @PostMapping("/code-check")
    @Operation(
            summary = "SKU 중복 확인",
            description = "신규 부품 등록 시 SKU 사용 가능 여부를 확인합니다. 중복은 에러가 아니라 available 값으로 반환합니다. ADMIN, HQ_MANAGER, HQ_STAFF 권한만 허용합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SKU 중복 확인 성공",
                    content = @Content(schema = @Schema(implementation = CodeCheckResponse.class))),
            @ApiResponse(responseCode = "400", description = "SKU 누락 또는 형식 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "401", description = "미인증 사용자",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "403", description = "SKU 중복 확인 권한 없음",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public CodeCheckResponse checkCode(@Valid @RequestBody CodeCheckRequest request) {
        String sku = request.normalizedSku();
        boolean available = itemService.isSkuAvailable(sku);

        return CodeCheckResponse.from(sku, available);
    }

    @GetMapping("/units")
    @Operation(
            summary = "부품 단위 목록 조회",
            description = "부품 등록/수정 모달에서 사용할 단위 목록을 조회합니다. 단위는 item-service 내부 enum으로 관리합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "부품 단위 목록 조회 성공",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ItemUnitResponse.class)))),
            @ApiResponse(responseCode = "401", description = "미인증 사용자",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public List<ItemUnitResponse> getUnits() {
        return itemService.getUnits().stream()
                .map(ItemUnitResponse::from)
                .toList();
    }

    private static String normalizeCategoryCode(String categoryCode) {
        String normalizedCategoryCode = ItemRequestValidator.trimToNull(categoryCode);
        if (normalizedCategoryCode == null) {
            return null;
        }
        return ItemRequestValidator.requireCategoryForFilter(normalizedCategoryCode);
    }

    private static Boolean parseStatus(String status) {
        String normalizedStatus = ItemRequestValidator.trimToNull(status);
        if (normalizedStatus == null) {
            return null;
        }
        return switch (normalizedStatus.toUpperCase()) {
            case "ALL" -> null;
            case "ACTIVE" -> true;
            case "INACTIVE" -> false;
            default -> throw new InvalidItemRequestException(ItemErrorCode.INVALID_PARAMETER, "Invalid status: " + status);
        };
    }

    private static int toZeroBasedPage(int page) {
        if (page < 1) {
            throw new InvalidItemRequestException(ItemErrorCode.INVALID_PARAMETER, "page must be greater than or equal to 1.");
        }
        return page - 1;
    }

    private static int requireSize(int size) {
        if (size < 1) {
            throw new InvalidItemRequestException(ItemErrorCode.INVALID_PARAMETER, "size must be greater than 0.");
        }
        return size;
    }

    private static ItemSortBy parseSortBy(String sort) {
        String[] parts = parseSort(sort);
        return switch (parts[0]) {
            case "sku" -> ItemSortBy.SKU;
            case "name" -> ItemSortBy.NAME;
            case "createdAt" -> ItemSortBy.CREATED_AT;
            case "updatedAt" -> ItemSortBy.UPDATED_AT;
            case "safetyStock" -> ItemSortBy.SAFETY_STOCK;
            default -> throw new InvalidItemRequestException(ItemErrorCode.INVALID_PARAMETER, "Invalid sort field: " + parts[0]);
        };
    }

    private static SortDirection parseSortDirection(String sort) {
        String[] parts = parseSort(sort);
        return switch (parts[1].toLowerCase()) {
            case "asc" -> SortDirection.ASC;
            case "desc" -> SortDirection.DESC;
            default -> throw new InvalidItemRequestException(ItemErrorCode.INVALID_PARAMETER, "Invalid sort direction: " + parts[1]);
        };
    }

    private static String[] parseSort(String sort) {
        String normalizedSort = ItemRequestValidator.trimToNull(sort);
        if (normalizedSort == null) {
            normalizedSort = DEFAULT_SORT;
        }
        String[] parts = normalizedSort.split(",");
        if (parts.length != 2 || parts[0].isBlank() || parts[1].isBlank()) {
            throw new InvalidItemRequestException(ItemErrorCode.INVALID_PARAMETER, "Invalid sort: " + sort);
        }
        return new String[]{parts[0].trim(), parts[1].trim()};
    }
}
