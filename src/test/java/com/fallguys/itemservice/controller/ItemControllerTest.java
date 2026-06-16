package com.fallguys.itemservice.controller;

import com.fallguys.itemservice.controller.security.JwtRoleConverter;
import com.fallguys.itemservice.controller.security.SecurityConfig;
import com.fallguys.itemservice.controller.security.SecurityProblemHandler;
import com.fallguys.itemservice.domain.CreateItemCommand;
import com.fallguys.itemservice.domain.Item;
import com.fallguys.itemservice.domain.ItemCategory;
import com.fallguys.itemservice.domain.ItemCategoryService;
import com.fallguys.itemservice.domain.ItemService;
import com.fallguys.itemservice.domain.ItemSortBy;
import com.fallguys.itemservice.domain.ItemUnit;
import com.fallguys.itemservice.domain.ItemView;
import com.fallguys.itemservice.domain.PageResult;
import com.fallguys.itemservice.domain.SearchItemsQuery;
import com.fallguys.itemservice.domain.SortDirection;
import com.fallguys.itemservice.domain.UpdateItemSelectionCommand;
import com.fallguys.itemservice.domain.exception.CategoryNotFoundException;
import com.fallguys.itemservice.domain.exception.DuplicateItemSkuException;
import com.fallguys.itemservice.domain.exception.InactiveItemCannotBeModifiedException;
import com.fallguys.itemservice.domain.exception.InventorySyncFailedException;
import com.fallguys.itemservice.domain.exception.InvalidItemStatusException;
import com.fallguys.itemservice.domain.exception.ItemNotFoundException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ItemController.class, ItemCategoryController.class, InternalItemController.class})
@Import({SecurityConfig.class, SecurityProblemHandler.class})
class ItemControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-06-06T10:30:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-06-07T10:30:00Z");
    private static final String VALID_CREATE_JSON = """
            {
              "sku": "HMC-EN-00214",
              "name": "엔진오일 필터 (2.0L gasoline)",
              "categoryCode": "ENGINE_LUBRICATION",
              "unit": "EA",
              "safetyStock": 120,
              "unitPrice": 15000
            }
            """;
    private static final String VALID_UPDATE_JSON = """
            {
              "name": "엔진오일 필터 (2.0L gasoline)",
              "categoryCode": "ENGINE",
              "subCategoryCode": "ENGINE_LUBRICATION",
              "unit": "EA",
              "unitPrice": 15000,
              "safetyStock": 120
            }
            """;

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private ItemCategoryService itemCategoryService;

    private static RequestPostProcessor adminJwt() {
        return roleJwt("ADMIN");
    }

    private static RequestPostProcessor roleJwt(String role) {
        return jwt()
                .jwt(jwt -> jwt.claim("user_role", role))
                .authorities(new JwtRoleConverter());
    }

    private static RequestPostProcessor jwtWithoutRole() {
        return jwt().authorities(new JwtRoleConverter());
    }

    private static MockHttpServletRequestBuilder createItemRequest() {
        return post("/items")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_CREATE_JSON);
    }

    private static MockHttpServletRequestBuilder updateItemRequest() {
        return patch("/items/{sku}", "HMC-EN-00214")
                .contentType(MediaType.APPLICATION_JSON)
                .content(VALID_UPDATE_JSON);
    }

    private static MockHttpServletRequestBuilder activateItemRequest() {
        return patch("/items/{sku}/activate", "HMC-WP-00229");
    }

    private static MockHttpServletRequestBuilder deactivateItemRequest() {
        return patch("/items/{sku}/deactivate", "HMC-WP-00229");
    }

    private static MockHttpServletRequestBuilder codeCheckRequest() {
        return post("/items/code-check")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "sku": "HMC-EN-00214"
                        }
                        """);
    }

    @Test
    void allowsWriteApisForAdminAndHeadOfficeRoles() throws Exception {
        when(itemService.createView(any(CreateItemCommand.class))).thenReturn(itemView());
        when(itemService.updateSelection(any(UpdateItemSelectionCommand.class))).thenReturn(itemView());
        when(itemService.activate(eq("HMC-WP-00229"))).thenReturn(statusItem(true));
        when(itemService.deactivate(eq("HMC-WP-00229"))).thenReturn(statusItem(false));
        when(itemService.isSkuAvailable(eq("HMC-EN-00214"))).thenReturn(true);

        for (String role : List.of("ADMIN", "HQ_MANAGER", "HQ_STAFF")) {
            mockMvc.perform(createItemRequest().with(roleJwt(role)))
                    .andExpect(status().isCreated());
            mockMvc.perform(updateItemRequest().with(roleJwt(role)))
                    .andExpect(status().isOk());
            mockMvc.perform(activateItemRequest().with(roleJwt(role)))
                    .andExpect(status().isOk());
            mockMvc.perform(deactivateItemRequest().with(roleJwt(role)))
                    .andExpect(status().isOk());
            mockMvc.perform(codeCheckRequest().with(roleJwt(role)))
                    .andExpect(status().isOk());
        }
    }

    @Test
    void rejectsWriteApisForBranchRoles() throws Exception {
        for (String role : List.of("BRANCH_MANAGER", "BRANCH_STAFF")) {
            mockMvc.perform(createItemRequest().with(roleJwt(role)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value("접근 권한이 없습니다."))
                    .andExpect(jsonPath("$.errorCode").doesNotExist());
            mockMvc.perform(updateItemRequest().with(roleJwt(role)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value("접근 권한이 없습니다."))
                    .andExpect(jsonPath("$.errorCode").doesNotExist());
            mockMvc.perform(activateItemRequest().with(roleJwt(role)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value("접근 권한이 없습니다."))
                    .andExpect(jsonPath("$.errorCode").doesNotExist());
            mockMvc.perform(deactivateItemRequest().with(roleJwt(role)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value("접근 권한이 없습니다."))
                    .andExpect(jsonPath("$.errorCode").doesNotExist());
            mockMvc.perform(codeCheckRequest().with(roleJwt(role)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.detail").value("접근 권한이 없습니다."))
                    .andExpect(jsonPath("$.errorCode").doesNotExist());
        }
    }

    @Test
    void rejectsUnauthenticatedWriteApis() throws Exception {
        mockMvc.perform(createItemRequest())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("인증이 필요합니다."))
                .andExpect(jsonPath("$.errorCode").doesNotExist());
        mockMvc.perform(updateItemRequest())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("인증이 필요합니다."))
                .andExpect(jsonPath("$.errorCode").doesNotExist());
        mockMvc.perform(activateItemRequest())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("인증이 필요합니다."))
                .andExpect(jsonPath("$.errorCode").doesNotExist());
        mockMvc.perform(deactivateItemRequest())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("인증이 필요합니다."))
                .andExpect(jsonPath("$.errorCode").doesNotExist());
        mockMvc.perform(codeCheckRequest())
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.detail").value("인증이 필요합니다."))
                .andExpect(jsonPath("$.errorCode").doesNotExist());
    }

    @Test
    void allowsReadApisForBranchStaff() throws Exception {
        when(itemService.searchViews(any(SearchItemsQuery.class)))
                .thenReturn(new PageResult<>(List.of(itemView()), 0, 10, 1));
        when(itemService.getViewBySku(eq("HMC-EN-00214"))).thenReturn(itemView());
        when(itemService.getUnits()).thenReturn(List.of(ItemUnit.EA));
        when(itemCategoryService.findRootCategories())
                .thenReturn(List.of(ItemCategory.root("ENGINE", "엔진", 1, true)));
        when(itemCategoryService.findSubCategories(eq("ENGINE")))
                .thenReturn(List.of(ItemCategory.subCategory("ENGINE_LUBRICATION", "윤활계통", "ENGINE", 1, true)));

        mockMvc.perform(get("/items").with(roleJwt("BRANCH_STAFF")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/items/{sku}", "HMC-EN-00214").with(roleJwt("BRANCH_STAFF")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/items/units").with(roleJwt("BRANCH_STAFF")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/items/categories").with(roleJwt("BRANCH_STAFF")))
                .andExpect(status().isOk());
        mockMvc.perform(get("/items/categories/{categoryCode}/sub-categories", "ENGINE").with(roleJwt("BRANCH_STAFF")))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsUserApisWhenJwtRoleClaimIsMissingOrUnknown() throws Exception {
        mockMvc.perform(get("/items").with(jwtWithoutRole()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("접근 권한이 없습니다."))
                .andExpect(jsonPath("$.errorCode").doesNotExist());
        mockMvc.perform(get("/items").with(roleJwt("UNKNOWN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.detail").value("접근 권한이 없습니다."))
                .andExpect(jsonPath("$.errorCode").doesNotExist());
    }

    @Test
    void searchesItemsWithDefaultsAndReturnsOneBasedPage() throws Exception {
        when(itemService.searchViews(any(SearchItemsQuery.class)))
                .thenReturn(new PageResult<>(List.of(itemView()), 0, 10, 11));

        mockMvc.perform(get("/items").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].sku").value("HMC-EN-00214"))
                .andExpect(jsonPath("$.content[0].categoryCode").value("ENGINE_LUBRICATION"))
                .andExpect(jsonPath("$.content[0].parentCategoryCode").value("ENGINE"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.size").value(10))
                .andExpect(jsonPath("$.totalElements").value(11))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.hasPrevious").value(false))
                .andExpect(jsonPath("$.hasNext").value(true));

        ArgumentCaptor<SearchItemsQuery> captor = ArgumentCaptor.forClass(SearchItemsQuery.class);
        verify(itemService).searchViews(captor.capture());
        SearchItemsQuery query = captor.getValue();
        org.junit.jupiter.api.Assertions.assertAll(
                () -> org.junit.jupiter.api.Assertions.assertEquals(0, query.page()),
                () -> org.junit.jupiter.api.Assertions.assertEquals(10, query.size()),
                () -> org.junit.jupiter.api.Assertions.assertNull(query.active()),
                () -> org.junit.jupiter.api.Assertions.assertEquals(ItemSortBy.UPDATED_AT, query.sortBy()),
                () -> org.junit.jupiter.api.Assertions.assertEquals(SortDirection.DESC, query.sortDirection())
        );
    }

    @Test
    void searchesItemsWithFilters() throws Exception {
        when(itemService.searchViews(any(SearchItemsQuery.class)))
                .thenReturn(new PageResult<>(List.of(), 1, 20, 0));

        mockMvc.perform(get("/items")
                        .param("search", " oil ")
                        .param("categoryCode", "ENGINE_LUBRICATION")
                        .param("status", "ACTIVE")
                        .param("page", "2")
                        .param("size", "20")
                        .param("sort", "safetyStock,asc")
                        .with(adminJwt()))
                .andExpect(status().isOk());

        ArgumentCaptor<SearchItemsQuery> captor = ArgumentCaptor.forClass(SearchItemsQuery.class);
        verify(itemService).searchViews(captor.capture());
        SearchItemsQuery query = captor.getValue();
        org.junit.jupiter.api.Assertions.assertAll(
                () -> org.junit.jupiter.api.Assertions.assertEquals("oil", query.search()),
                () -> org.junit.jupiter.api.Assertions.assertEquals("ENGINE_LUBRICATION", query.categoryCode()),
                () -> org.junit.jupiter.api.Assertions.assertEquals(true, query.active()),
                () -> org.junit.jupiter.api.Assertions.assertEquals(1, query.page()),
                () -> org.junit.jupiter.api.Assertions.assertEquals(20, query.size()),
                () -> org.junit.jupiter.api.Assertions.assertEquals(ItemSortBy.SAFETY_STOCK, query.sortBy()),
                () -> org.junit.jupiter.api.Assertions.assertEquals(SortDirection.ASC, query.sortDirection())
        );
    }

    @Test
    void failsWhenSearchParameterIsInvalid() throws Exception {
        mockMvc.perform(get("/items").param("status", "UNKNOWN").with(adminJwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ITM-001"))
                .andExpect(jsonPath("$.timestamp").exists());

        mockMvc.perform(get("/items").param("categoryCode", "engine").with(adminJwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ITM-002"));
    }

    @Test
    void getsItemDetailBySku() throws Exception {
        when(itemService.getViewBySku(eq("HMC-EN-00214"))).thenReturn(itemView());

        mockMvc.perform(get("/items/{sku}", "HMC-EN-00214").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("HMC-EN-00214"))
                .andExpect(jsonPath("$.name").value("엔진오일 필터 (2.0L gasoline)"))
                .andExpect(jsonPath("$.categoryCode").value("ENGINE"))
                .andExpect(jsonPath("$.categoryName").value("엔진"))
                .andExpect(jsonPath("$.subCategoryCode").value("ENGINE_LUBRICATION"))
                .andExpect(jsonPath("$.subCategoryName").value("윤활계통"))
                .andExpect(jsonPath("$.unit").value("EA"))
                .andExpect(jsonPath("$.unitPrice").value(15000))
                .andExpect(jsonPath("$.safetyStock").value(120))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.createdAt").value("2026-06-06"))
                .andExpect(jsonPath("$.updatedAt").value("2026-06-07"));

        verify(itemService).getViewBySku("HMC-EN-00214");
    }

    @Test
    void failsWhenDetailSkuIsInvalidOrMissing() throws Exception {
        mockMvc.perform(get("/items/{sku}", "hmc.wp").with(adminJwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ITM-006"));

        when(itemService.getViewBySku(eq("UNKNOWN")))
                .thenThrow(new ItemNotFoundException("UNKNOWN"));

        mockMvc.perform(get("/items/{sku}", "UNKNOWN").with(adminJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ITM-019"));
    }

    @Test
    void getsInternalItemBySku() throws Exception {
        when(itemService.getViewBySku(eq("HMC-EN-00214"))).thenReturn(itemView());

        mockMvc.perform(get("/internal/items/{sku}", "HMC-EN-00214"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("HMC-EN-00214"))
                .andExpect(jsonPath("$.name").value("엔진오일 필터 (2.0L gasoline)"))
                .andExpect(jsonPath("$.majorCategory").value("엔진"))
                .andExpect(jsonPath("$.middleCategory").value("윤활계통"))
                .andExpect(jsonPath("$.unit").value("EA"))
                .andExpect(jsonPath("$.unitPrice").value(15000))
                .andExpect(jsonPath("$.safetyStock").value(120))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.categoryCode").doesNotExist())
                .andExpect(jsonPath("$.categoryName").doesNotExist())
                .andExpect(jsonPath("$.subCategoryCode").doesNotExist())
                .andExpect(jsonPath("$.subCategoryName").doesNotExist())
                .andExpect(jsonPath("$.createdAt").doesNotExist())
                .andExpect(jsonPath("$.updatedAt").doesNotExist());

        verify(itemService).getViewBySku("HMC-EN-00214");
    }

    @Test
    void getsInternalItemBySkuWhenCategoryHasNoParent() throws Exception {
        when(itemService.getViewBySku(eq("HMC-EN-00001"))).thenReturn(rootCategoryItemView());

        mockMvc.perform(get("/internal/items/{sku}", "HMC-EN-00001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("HMC-EN-00001"))
                .andExpect(jsonPath("$.majorCategory").value("엔진"))
                .andExpect(jsonPath("$.middleCategory").value(""));

        verify(itemService).getViewBySku("HMC-EN-00001");
    }

    @Test
    void getsInternalItemCategoryBySku() throws Exception {
        when(itemService.getViewBySku(eq("HMC-EN-00214"))).thenReturn(itemView());
        when(itemService.getViewBySku(eq("HMC-EN-00001"))).thenReturn(rootCategoryItemView());

        mockMvc.perform(get("/internal/items/category/{sku}", "HMC-EN-00214"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("HMC-EN-00214"))
                .andExpect(jsonPath("$.majorCategory").value("엔진"))
                .andExpect(jsonPath("$.middleCategory").value("윤활계통"))
                .andExpect(jsonPath("$.name").doesNotExist())
                .andExpect(jsonPath("$.unit").doesNotExist())
                .andExpect(jsonPath("$.active").doesNotExist());

        mockMvc.perform(get("/internal/items/category/{sku}", "HMC-EN-00001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("HMC-EN-00001"))
                .andExpect(jsonPath("$.majorCategory").value("엔진"))
                .andExpect(jsonPath("$.middleCategory").value(""));

        verify(itemService).getViewBySku("HMC-EN-00214");
        verify(itemService).getViewBySku("HMC-EN-00001");
    }

    @Test
    void failsWhenInternalSkuIsInvalidOrMissing() throws Exception {
        mockMvc.perform(get("/internal/items/{sku}", "hmc.wp"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ITM-006"));

        when(itemService.getViewBySku(eq("UNKNOWN")))
                .thenThrow(new ItemNotFoundException("UNKNOWN"));

        mockMvc.perform(get("/internal/items/{sku}", "UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ITM-019"));
    }

    @Test
    void failsWhenInternalCategorySkuIsInvalidOrMissing() throws Exception {
        mockMvc.perform(get("/internal/items/category/{sku}", "hmc.wp"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ITM-006"));

        when(itemService.getViewBySku(eq("UNKNOWN")))
                .thenThrow(new ItemNotFoundException("UNKNOWN"));

        mockMvc.perform(get("/internal/items/category/{sku}", "UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ITM-019"));
    }

    @Test
    void getsInternalItemsBySkus() throws Exception {
        when(itemService.getBySkus(eq(List.of("HMC-WP-00229", "HMC-NO-99999", "HMC-EN-00214"))))
                .thenReturn(List.of(internalItem(), internalItem("HMC-WP-00229", "워터 펌프 어셈블리")));

        mockMvc.perform(post("/internal/items/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "skus": ["HMC-WP-00229", "HMC-NO-99999", "HMC-EN-00214"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items[0].sku").value("HMC-WP-00229"))
                .andExpect(jsonPath("$.items[1].sku").value("HMC-EN-00214"))
                .andExpect(jsonPath("$.items[1].name").value("엔진오일 필터 (2.0L gasoline)"))
                .andExpect(jsonPath("$.items[1].categoryCode").value("ENGINE_LUBRICATION"))
                .andExpect(jsonPath("$.items[1].unit").value("EA"))
                .andExpect(jsonPath("$.items[1].unitPrice").value(15000))
                .andExpect(jsonPath("$.items[1].safetyStock").value(120))
                .andExpect(jsonPath("$.items[1].active").value(true))
                .andExpect(jsonPath("$.items[1].categoryName").doesNotExist())
                .andExpect(jsonPath("$.items[1].subCategoryCode").doesNotExist())
                .andExpect(jsonPath("$.items[1].createdAt").doesNotExist())
                .andExpect(jsonPath("$.items[1].updatedAt").doesNotExist())
                .andExpect(jsonPath("$.notFoundSkus[0]").value("HMC-NO-99999"));

        verify(itemService).getBySkus(List.of("HMC-WP-00229", "HMC-NO-99999", "HMC-EN-00214"));
    }

    @Test
    void deduplicatesInternalBatchSkus() throws Exception {
        when(itemService.getBySkus(eq(List.of("HMC-EN-00214", "HMC-WP-00229"))))
                .thenReturn(List.of(internalItem(), internalItem("HMC-WP-00229", "워터 펌프 어셈블리")));

        mockMvc.perform(post("/internal/items/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "skus": ["HMC-EN-00214", "HMC-EN-00214", "HMC-WP-00229"]
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].sku").value("HMC-EN-00214"))
                .andExpect(jsonPath("$.items[1].sku").value("HMC-WP-00229"))
                .andExpect(jsonPath("$.notFoundSkus.length()").value(0));

        verify(itemService).getBySkus(List.of("HMC-EN-00214", "HMC-WP-00229"));
    }

    @Test
    void failsWhenInternalBatchSkusAreMissingOrInvalid() throws Exception {
        mockMvc.perform(post("/internal/items/batch"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ITM-005"));

        mockMvc.perform(post("/internal/items/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ITM-005"));

        mockMvc.perform(post("/internal/items/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "skus": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ITM-005"));

        mockMvc.perform(post("/internal/items/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "skus": ["HMC-EN-00214", "hmc.wp"]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ITM-006"));
    }

    @Test
    void failsWhenInternalBatchHasTooManySkus() throws Exception {
        String content = java.util.stream.IntStream.rangeClosed(1, 101)
                .mapToObj(number -> "\"HMC-EN-" + String.format("%05d", number) + "\"")
                .collect(java.util.stream.Collectors.joining(",", "{\"skus\":[", "]}"));

        mockMvc.perform(post("/internal/items/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(content))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ITM-007"));
    }

    @Test
    void createsItem() throws Exception {
        when(itemService.createView(any(CreateItemCommand.class))).thenReturn(itemView());

        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "HMC-EN-00214",
                                  "name": "엔진오일 필터 (2.0L gasoline)",
                                  "categoryCode": "ENGINE_LUBRICATION",
                                  "unit": "EA",
                                  "safetyStock": 120,
                                  "unitPrice": 15000
                                }
                                """)
                        .with(adminJwt()))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("HMC-EN-00214"))
                .andExpect(jsonPath("$.parentCategoryCode").value("ENGINE"))
                .andExpect(jsonPath("$.categoryCode").value("ENGINE_LUBRICATION"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void failsWhenCreateRequestIsInvalidOrDuplicated() throws Exception {
        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "엔진오일 필터",
                                  "categoryCode": "ENGINE_LUBRICATION",
                                  "unit": "EA",
                                  "safetyStock": 120,
                                  "unitPrice": 15000
                                }
                                """)
                        .with(adminJwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ITM-004"));

        when(itemService.createView(any(CreateItemCommand.class)))
                .thenThrow(new DuplicateItemSkuException("HMC-EN-00214"));

        mockMvc.perform(post("/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "HMC-EN-00214",
                                  "name": "엔진오일 필터",
                                  "categoryCode": "ENGINE_LUBRICATION",
                                  "unit": "EA",
                                  "safetyStock": 120,
                                  "unitPrice": 15000
                                }
                                """)
                        .with(adminJwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ITM-013"));
    }

    @Test
    void updatesItem() throws Exception {
        when(itemService.updateSelection(any(UpdateItemSelectionCommand.class))).thenReturn(itemView());

        mockMvc.perform(patch("/items/{sku}", "HMC-EN-00214")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "엔진오일 필터 (2.0L gasoline)",
                                  "categoryCode": "ENGINE",
                                  "subCategoryCode": "ENGINE_LUBRICATION",
                                  "unit": "EA",
                                  "unitPrice": 15000,
                                  "safetyStock": 120
                                }
                                """)
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryCode").value("ENGINE"))
                .andExpect(jsonPath("$.subCategoryCode").value("ENGINE_LUBRICATION"));
    }

    @Test
    void failsWhenInactiveItemIsModified() throws Exception {
        when(itemService.updateSelection(any(UpdateItemSelectionCommand.class)))
                .thenThrow(new InactiveItemCannotBeModifiedException("HMC-EN-00214"));

        mockMvc.perform(patch("/items/{sku}", "HMC-EN-00214")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "엔진오일 필터",
                                  "categoryCode": "ENGINE",
                                  "subCategoryCode": "ENGINE_LUBRICATION",
                                  "unit": "EA",
                                  "unitPrice": 15000,
                                  "safetyStock": 120
                                }
                                """)
                        .with(adminJwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ITM-018"));
    }

    @Test
    void mapsInventorySyncFailureToBadGateway() throws Exception {
        when(itemService.updateSelection(any(UpdateItemSelectionCommand.class)))
                .thenThrow(new InventorySyncFailedException(
                        "재고 서비스 동기화 요청이 거부되었습니다: itemName",
                        new RuntimeException("forbidden")
                ));

        mockMvc.perform(updateItemRequest().with(adminJwt()))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.errorCode").value("ITM-022"));
    }

    @Test
    void activatesItem() throws Exception {
        when(itemService.activate(eq("HMC-WP-00229"))).thenReturn(statusItem(true));

        mockMvc.perform(patch("/items/{sku}/activate", "HMC-WP-00229").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("HMC-WP-00229"))
                .andExpect(jsonPath("$.name").value("워터 펌프 어셈블리 (구형)"))
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.updatedAt").value("2026-06-07T10:30"));

        verify(itemService).activate("HMC-WP-00229");
    }

    @Test
    void deactivatesItem() throws Exception {
        when(itemService.deactivate(eq("HMC-WP-00229"))).thenReturn(statusItem(false));

        mockMvc.perform(patch("/items/{sku}/deactivate", "HMC-WP-00229").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("HMC-WP-00229"))
                .andExpect(jsonPath("$.active").value(false))
                .andExpect(jsonPath("$.updatedAt").value("2026-06-07T10:30"));

        verify(itemService).deactivate("HMC-WP-00229");
    }

    @Test
    void failsWhenStatusChangeSkuIsInvalid() throws Exception {
        mockMvc.perform(patch("/items/{sku}/activate", "hmc.wp").with(adminJwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ITM-006"));
    }

    @Test
    void failsWhenItemStatusIsAlreadyApplied() throws Exception {
        when(itemService.activate(eq("HMC-WP-00229")))
                .thenThrow(InvalidItemStatusException.alreadyActive("HMC-WP-00229"));

        mockMvc.perform(patch("/items/{sku}/activate", "HMC-WP-00229").with(adminJwt()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("ITM-017"));
    }

    @Test
    void mapsConcurrentStatusChangeToConflict() throws Exception {
        when(itemService.deactivate(eq("HMC-WP-00229")))
                .thenThrow(new ObjectOptimisticLockingFailureException(Item.class, "HMC-WP-00229"));

        mockMvc.perform(patch("/items/{sku}/deactivate", "HMC-WP-00229").with(adminJwt()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("ITM-020"));
    }

    @Test
    void checksSkuAvailability() throws Exception {
        when(itemService.isSkuAvailable(eq("HMC-EN-00214"))).thenReturn(false);

        mockMvc.perform(post("/items/code-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "HMC-EN-00214"
                                }
                                """)
                        .with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("HMC-EN-00214"))
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 부품코드입니다."));
    }

    @Test
    void findsUnits() throws Exception {
        when(itemService.getUnits()).thenReturn(List.of(ItemUnit.EA, ItemUnit.BOX, ItemUnit.SET, ItemUnit.L));

        mockMvc.perform(get("/items/units").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].unit").value("EA"))
                .andExpect(jsonPath("$[0].name").value("EA"))
                .andExpect(jsonPath("$[1].unit").value("BOX"))
                .andExpect(jsonPath("$[1].name").value("BOX"))
                .andExpect(jsonPath("$[2].unit").value("SET"))
                .andExpect(jsonPath("$[2].name").value("SET"))
                .andExpect(jsonPath("$[3].unit").value("L"))
                .andExpect(jsonPath("$[3].name").value("L"));

        verify(itemService).getUnits();
    }

    @Test
    void findsCategories() throws Exception {
        when(itemCategoryService.findRootCategories())
                .thenReturn(List.of(ItemCategory.root("ENGINE", "엔진", 1, true)));

        mockMvc.perform(get("/items/categories").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryCode").value("ENGINE"))
                .andExpect(jsonPath("$[0].categoryName").value("엔진"))
                .andExpect(jsonPath("$[0].displayOrder").value(1));
    }

    @Test
    void findsSubCategoriesAndHandlesNotFound() throws Exception {
        when(itemCategoryService.findSubCategories(eq("ENGINE")))
                .thenReturn(List.of(ItemCategory.subCategory("ENGINE_LUBRICATION", "윤활계통", "ENGINE", 1, true)));

        mockMvc.perform(get("/items/categories/{categoryCode}/sub-categories", "ENGINE").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryCode").value("ENGINE_LUBRICATION"))
                .andExpect(jsonPath("$[0].parentCategoryCode").value("ENGINE"));

        when(itemCategoryService.findSubCategories(eq("UNKNOWN")))
                .thenThrow(new CategoryNotFoundException("UNKNOWN"));

        mockMvc.perform(get("/items/categories/{categoryCode}/sub-categories", "UNKNOWN").with(adminJwt()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("ITM-003"));
    }

    @Test
    void servesItemApisUnderGatewayStrippedPrefix() throws Exception {
        when(itemService.searchViews(any(SearchItemsQuery.class)))
                .thenReturn(new PageResult<>(List.of(itemView()), 0, 10, 1));
        when(itemCategoryService.findRootCategories())
                .thenReturn(List.of(ItemCategory.root("ENGINE", "엔진", 1, true)));
        when(itemCategoryService.findSubCategories(eq("ENGINE")))
                .thenReturn(List.of(ItemCategory.subCategory("ENGINE_LUBRICATION", "윤활계통", "ENGINE", 1, true)));
        when(itemService.activate(eq("HMC-WP-00229"))).thenReturn(statusItem(true));
        when(itemService.deactivate(eq("HMC-WP-00229"))).thenReturn(statusItem(false));
        when(itemService.getViewBySku(eq("HMC-EN-00214"))).thenReturn(itemView());
        when(itemService.getUnits()).thenReturn(List.of(ItemUnit.EA, ItemUnit.BOX, ItemUnit.SET, ItemUnit.L));

        mockMvc.perform(get("/items").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));
        mockMvc.perform(get("/items/{sku}", "HMC-EN-00214").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.subCategoryCode").value("ENGINE_LUBRICATION"));
        mockMvc.perform(get("/items/units").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[1].unit").value("BOX"));
        mockMvc.perform(get("/items/categories").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryCode").value("ENGINE"));
        mockMvc.perform(get("/items/categories/{categoryCode}/sub-categories", "ENGINE").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryCode").value("ENGINE_LUBRICATION"));
        mockMvc.perform(patch("/items/{sku}/activate", "HMC-WP-00229").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true));
        mockMvc.perform(patch("/items/{sku}/deactivate", "HMC-WP-00229").with(adminJwt()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(false));
    }

    private static Item statusItem(boolean active) {
        return Item.of(
                "HMC-WP-00229",
                "워터 펌프 어셈블리 (구형)",
                "ENGINE_LUBRICATION",
                ItemUnit.EA,
                120,
                15000,
                active,
                CREATED_AT,
                UPDATED_AT
        );
    }

    private static ItemView itemView() {
        return new ItemView(
                "HMC-EN-00214",
                "엔진오일 필터 (2.0L gasoline)",
                "ENGINE_LUBRICATION",
                "윤활계통",
                "ENGINE",
                "엔진",
                ItemUnit.EA,
                120,
                15000,
                true,
                CREATED_AT,
                UPDATED_AT
        );
    }

    private static ItemView rootCategoryItemView() {
        return new ItemView(
                "HMC-EN-00001",
                "엔진 어셈블리",
                "ENGINE",
                "엔진",
                null,
                null,
                ItemUnit.EA,
                120,
                15000,
                true,
                CREATED_AT,
                UPDATED_AT
        );
    }

    private static Item internalItem() {
        return internalItem("HMC-EN-00214", "엔진오일 필터 (2.0L gasoline)");
    }

    private static Item internalItem(String sku, String name) {
        return Item.of(
                sku,
                name,
                "ENGINE_LUBRICATION",
                ItemUnit.EA,
                120,
                15000,
                true,
                CREATED_AT,
                UPDATED_AT
        );
    }
}
