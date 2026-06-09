package com.fallguys.itemservice.controller;

import com.fallguys.itemservice.controller.dto.ItemCategoryResponse;
import com.fallguys.itemservice.controller.dto.ItemRequestValidator;
import com.fallguys.itemservice.controller.dto.ItemSubCategoryResponse;
import com.fallguys.itemservice.domain.ItemCategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RequestMapping("/items/categories")
@RestController
@Tag(name = "Item Categories", description = "부품 대분류와 중분류 조회 API")
public class ItemCategoryController {

    private final ItemCategoryService itemCategoryService;

    public ItemCategoryController(ItemCategoryService itemCategoryService) {
        this.itemCategoryService = itemCategoryService;
    }

    @GetMapping
    @Operation(
            summary = "대분류 목록 조회",
            description = "부품 목록 필터와 부품 등록 모달에서 사용할 활성 대분류 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "대분류 목록 조회 성공",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ItemCategoryResponse.class)))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public List<ItemCategoryResponse> findRootCategories() {
        return itemCategoryService.findRootCategories().stream()
                .map(ItemCategoryResponse::from)
                .toList();
    }

    @GetMapping("/{categoryCode}/sub-categories")
    @Operation(
            summary = "중분류 목록 조회",
            description = "대분류 선택 시 해당 대분류 하위의 활성 중분류 목록을 조회합니다."
    )
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "중분류 목록 조회 성공",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = ItemSubCategoryResponse.class)))),
            @ApiResponse(responseCode = "400", description = "categoryCode 형식 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "404", description = "존재하지 않는 대분류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @ApiResponse(responseCode = "500", description = "서버 내부 오류",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public List<ItemSubCategoryResponse> findSubCategories(
            @Parameter(description = "대분류 코드", example = "ENGINE")
            @PathVariable String categoryCode
    ) {
        String normalizedCategoryCode = ItemRequestValidator.requireCategoryForFilter(categoryCode);

        return itemCategoryService.findSubCategories(normalizedCategoryCode).stream()
                .map(ItemSubCategoryResponse::from)
                .toList();
    }
}
