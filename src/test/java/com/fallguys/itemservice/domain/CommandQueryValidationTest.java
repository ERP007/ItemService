package com.fallguys.itemservice.domain;

import com.fallguys.itemservice.domain.exception.InvalidItemException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CommandQueryValidationTest {

    @Test
    void createItemCommandNormalizesRequiredText() {
        CreateItemCommand command = new CreateItemCommand(
                " ENG-OIL-5W30-1L ",
                " Engine oil ",
                " ENGINE_OIL ",
                ItemUnit.EA,
                50,
                8500
        );

        assertAll(
                () -> assertEquals("ENG-OIL-5W30-1L", command.sku()),
                () -> assertEquals("Engine oil", command.name()),
                () -> assertEquals("ENGINE_OIL", command.categoryCode())
        );
    }

    @Test
    void createItemCommandFailsWhenInputIsInvalid() {
        assertAll(
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> new CreateItemCommand(null, "Engine oil", "ENGINE_OIL", ItemUnit.EA, 50, 8500)
                ),
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> new CreateItemCommand(" ", "Engine oil", "ENGINE_OIL", ItemUnit.EA, 50, 8500)
                ),
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> new CreateItemCommand("ENG-OIL-5W30-1L", null, "ENGINE_OIL", ItemUnit.EA, 50, 8500)
                ),
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> new CreateItemCommand("ENG-OIL-5W30-1L", "Engine oil", "", ItemUnit.EA, 50, 8500)
                ),
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> new CreateItemCommand("ENG-OIL-5W30-1L", "Engine oil", "ENGINE_OIL", null, 50, 8500)
                ),
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> new CreateItemCommand("ENG-OIL-5W30-1L", "Engine oil", "ENGINE_OIL", ItemUnit.EA, -1, 8500)
                ),
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> new CreateItemCommand("ENG-OIL-5W30-1L", "Engine oil", "ENGINE_OIL", ItemUnit.EA, 50, -1)
                )
        );
    }

    @Test
    void updateItemCommandNormalizesRequiredText() {
        UpdateItemCommand command = new UpdateItemCommand(
                " ENG-OIL-5W30-1L ",
                " Engine oil ",
                " ENGINE_OIL ",
                ItemUnit.EA,
                50,
                8500
        );

        assertAll(
                () -> assertEquals("ENG-OIL-5W30-1L", command.sku()),
                () -> assertEquals("Engine oil", command.name()),
                () -> assertEquals("ENGINE_OIL", command.categoryCode())
        );
    }

    @Test
    void updateItemCommandFailsWhenInputIsInvalid() {
        assertAll(
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> new UpdateItemCommand(null, "Engine oil", "ENGINE_OIL", ItemUnit.EA, 50, 8500)
                ),
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> new UpdateItemCommand(" ", "Engine oil", "ENGINE_OIL", ItemUnit.EA, 50, 8500)
                ),
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> new UpdateItemCommand("ENG-OIL-5W30-1L", null, "ENGINE_OIL", ItemUnit.EA, 50, 8500)
                ),
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> new UpdateItemCommand("ENG-OIL-5W30-1L", "Engine oil", "", ItemUnit.EA, 50, 8500)
                ),
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> new UpdateItemCommand("ENG-OIL-5W30-1L", "Engine oil", "ENGINE_OIL", null, 50, 8500)
                ),
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> new UpdateItemCommand("ENG-OIL-5W30-1L", "Engine oil", "ENGINE_OIL", ItemUnit.EA, -1, 8500)
                ),
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> new UpdateItemCommand("ENG-OIL-5W30-1L", "Engine oil", "ENGINE_OIL", ItemUnit.EA, 50, -1)
                )
        );
    }

    @Test
    void updateItemSelectionCommandNormalizesOptionalSubCategory() {
        UpdateItemSelectionCommand withoutSubCategory = new UpdateItemSelectionCommand(
                " ENG-OIL-5W30-1L ",
                " Engine oil ",
                " ENGINE ",
                " ",
                ItemUnit.EA,
                50,
                8500
        );
        UpdateItemSelectionCommand withSubCategory = new UpdateItemSelectionCommand(
                " ENG-OIL-5W30-1L ",
                " Engine oil ",
                " ENGINE ",
                " ENGINE_OIL ",
                ItemUnit.EA,
                50,
                8500
        );

        assertAll(
                () -> assertEquals("ENG-OIL-5W30-1L", withoutSubCategory.sku()),
                () -> assertEquals("Engine oil", withoutSubCategory.name()),
                () -> assertEquals("ENGINE", withoutSubCategory.categoryCode()),
                () -> assertNull(withoutSubCategory.subCategoryCode()),
                () -> assertEquals("ENGINE_OIL", withSubCategory.subCategoryCode())
        );
    }

    @Test
    void updateItemSelectionCommandFailsWhenInputIsInvalid() {
        assertAll(
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> new UpdateItemSelectionCommand(null, "Engine oil", "ENGINE", null, ItemUnit.EA, 50, 8500)
                ),
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> new UpdateItemSelectionCommand(" ", "Engine oil", "ENGINE", null, ItemUnit.EA, 50, 8500)
                ),
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> new UpdateItemSelectionCommand("ENG-OIL-5W30-1L", null, "ENGINE", null, ItemUnit.EA, 50, 8500)
                ),
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> new UpdateItemSelectionCommand("ENG-OIL-5W30-1L", "Engine oil", "", null, ItemUnit.EA, 50, 8500)
                ),
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> new UpdateItemSelectionCommand("ENG-OIL-5W30-1L", "Engine oil", "ENGINE", null, null, 50, 8500)
                ),
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> new UpdateItemSelectionCommand("ENG-OIL-5W30-1L", "Engine oil", "ENGINE", null, ItemUnit.EA, -1, 8500)
                ),
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> new UpdateItemSelectionCommand("ENG-OIL-5W30-1L", "Engine oil", "ENGINE", null, ItemUnit.EA, 50, -1)
                )
        );
    }

    @Test
    void searchItemViewsQueryNormalizesSearchAndDefaultsSort() {
        SearchItemViewsQuery blankSearch = new SearchItemViewsQuery(" ", List.of("ENGINE"), null, 0, 20, null, null);
        SearchItemViewsQuery keywordSearch = new SearchItemViewsQuery(
                " oil ",
                List.of("ENGINE"),
                true,
                1,
                10,
                ItemSortBy.SKU,
                SortDirection.ASC
        );

        assertAll(
                () -> assertNull(blankSearch.search()),
                () -> assertEquals(ItemSortBy.UPDATED_AT, blankSearch.sortBy()),
                () -> assertEquals(SortDirection.DESC, blankSearch.sortDirection()),
                () -> assertEquals("oil", keywordSearch.search()),
                () -> assertEquals(List.of("ENGINE"), keywordSearch.categoryCodes()),
                () -> assertEquals(true, keywordSearch.active()),
                () -> assertEquals(ItemSortBy.SKU, keywordSearch.sortBy()),
                () -> assertEquals(SortDirection.ASC, keywordSearch.sortDirection())
        );
    }

    @Test
    void searchItemViewsQueryFailsWhenPagingInputIsInvalid() {
        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new SearchItemViewsQuery(null, null, null, 0, 20, null, null)
                ),
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> new SearchItemViewsQuery(null, List.of(), null, -1, 20, null, null)
                ),
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> new SearchItemViewsQuery(null, List.of(), null, 0, 0, null, null)
                )
        );
    }
}
