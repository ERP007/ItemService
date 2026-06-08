package com.fallguys.itemservice.controller;

import com.fallguys.itemservice.domain.CreateItemCommand;
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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = {ItemController.class, ItemCategoryController.class})
class ItemControllerTest {

    private static final Instant CREATED_AT = Instant.parse("2026-06-06T10:30:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-06-07T10:30:00Z");

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ItemService itemService;

    @MockitoBean
    private ItemCategoryService itemCategoryService;

    @Test
    void searchesItemsWithDefaultsAndReturnsOneBasedPage() throws Exception {
        when(itemService.searchViews(any(SearchItemsQuery.class)))
                .thenReturn(new PageResult<>(List.of(itemView()), 0, 10, 11));

        mockMvc.perform(get("/api/items"))
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

        mockMvc.perform(get("/api/items")
                        .param("search", " oil ")
                        .param("categoryCode", "ENGINE_LUBRICATION")
                        .param("status", "ACTIVE")
                        .param("page", "2")
                        .param("size", "20")
                        .param("sort", "safetyStock,asc"))
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
        mockMvc.perform(get("/api/items").param("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_PARAMETER"))
                .andExpect(jsonPath("$.timestamp").exists());

        mockMvc.perform(get("/api/items").param("categoryCode", "engine"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INVALID_CATEGORY_CODE"));
    }

    @Test
    void createsItem() throws Exception {
        when(itemService.createView(any(CreateItemCommand.class))).thenReturn(itemView());

        mockMvc.perform(post("/api/items")
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
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.sku").value("HMC-EN-00214"))
                .andExpect(jsonPath("$.parentCategoryCode").value("ENGINE"))
                .andExpect(jsonPath("$.categoryCode").value("ENGINE_LUBRICATION"))
                .andExpect(jsonPath("$.active").value(true));
    }

    @Test
    void failsWhenCreateRequestIsInvalidOrDuplicated() throws Exception {
        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "엔진오일 필터",
                                  "categoryCode": "ENGINE_LUBRICATION",
                                  "unit": "EA",
                                  "safetyStock": 120,
                                  "unitPrice": 15000
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("SKU_REQUIRED"));

        when(itemService.createView(any(CreateItemCommand.class)))
                .thenThrow(new DuplicateItemSkuException("HMC-EN-00214"));

        mockMvc.perform(post("/api/items")
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
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.errorCode").value("DUPLICATE_SKU"));
    }

    @Test
    void updatesItem() throws Exception {
        when(itemService.updateSelection(any(UpdateItemSelectionCommand.class))).thenReturn(itemView());

        mockMvc.perform(patch("/api/items/{sku}", "HMC-EN-00214")
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
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categoryCode").value("ENGINE"))
                .andExpect(jsonPath("$.subCategoryCode").value("ENGINE_LUBRICATION"));
    }

    @Test
    void failsWhenInactiveItemIsModified() throws Exception {
        when(itemService.updateSelection(any(UpdateItemSelectionCommand.class)))
                .thenThrow(new InactiveItemCannotBeModifiedException("HMC-EN-00214"));

        mockMvc.perform(patch("/api/items/{sku}", "HMC-EN-00214")
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
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errorCode").value("INACTIVE_ITEM_CANNOT_BE_MODIFIED"));
    }

    @Test
    void checksSkuAvailability() throws Exception {
        when(itemService.isSkuAvailable(eq("HMC-EN-00214"))).thenReturn(false);

        mockMvc.perform(post("/api/items/code-check")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "sku": "HMC-EN-00214"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("HMC-EN-00214"))
                .andExpect(jsonPath("$.available").value(false))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 부품코드입니다."));
    }

    @Test
    void findsCategories() throws Exception {
        when(itemCategoryService.findRootCategories())
                .thenReturn(List.of(ItemCategory.root("ENGINE", "엔진", 1, true)));

        mockMvc.perform(get("/api/items/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryCode").value("ENGINE"))
                .andExpect(jsonPath("$[0].categoryName").value("엔진"))
                .andExpect(jsonPath("$[0].displayOrder").value(1));
    }

    @Test
    void findsSubCategoriesAndHandlesNotFound() throws Exception {
        when(itemCategoryService.findSubCategories(eq("ENGINE")))
                .thenReturn(List.of(ItemCategory.subCategory("ENGINE_LUBRICATION", "윤활계통", "ENGINE", 1, true)));

        mockMvc.perform(get("/api/items/categories/{categoryCode}/sub-categories", "ENGINE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].categoryCode").value("ENGINE_LUBRICATION"))
                .andExpect(jsonPath("$[0].parentCategoryCode").value("ENGINE"));

        when(itemCategoryService.findSubCategories(eq("UNKNOWN")))
                .thenThrow(new CategoryNotFoundException("UNKNOWN"));

        mockMvc.perform(get("/api/items/categories/{categoryCode}/sub-categories", "UNKNOWN"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.errorCode").value("CATEGORY_NOT_FOUND"));
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
}
