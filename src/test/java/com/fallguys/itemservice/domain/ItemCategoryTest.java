package com.fallguys.itemservice.domain;

import com.fallguys.itemservice.domain.exception.InvalidItemCategoryException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemCategoryTest {

    @Test
    void createsRootCategory() {
        ItemCategory category = ItemCategory.root(" ENGINE ", " Engine ", 1, true);

        assertAll(
                () -> assertEquals("ENGINE", category.getCode()),
                () -> assertEquals("Engine", category.getName()),
                () -> assertNull(category.getParentCode()),
                () -> assertEquals(1, category.getDepth()),
                () -> assertEquals(1, category.getDisplayOrder()),
                () -> assertTrue(category.isActive())
        );
    }

    @Test
    void createsSubCategory() {
        ItemCategory category = ItemCategory.subCategory("ENGINE_OIL", "Engine oil", " ENGINE ", 2, false);

        assertAll(
                () -> assertEquals("ENGINE_OIL", category.getCode()),
                () -> assertEquals("Engine oil", category.getName()),
                () -> assertEquals("ENGINE", category.getParentCode()),
                () -> assertEquals(2, category.getDepth()),
                () -> assertEquals(2, category.getDisplayOrder()),
                () -> assertFalse(category.isActive())
        );
    }

    @Test
    void failsWhenRootCategoryHasParentCode() {
        assertThrows(
                InvalidItemCategoryException.class,
                () -> ItemCategory.of("ENGINE", "Engine", "ROOT", 1, 0, true)
        );
    }

    @Test
    void failsWhenSubCategoryHasNoParentCode() {
        assertAll(
                () -> assertThrows(
                        InvalidItemCategoryException.class,
                        () -> ItemCategory.of("ENGINE_OIL", "Engine oil", null, 2, 0, true)
                ),
                () -> assertThrows(
                        InvalidItemCategoryException.class,
                        () -> ItemCategory.of("ENGINE_OIL", "Engine oil", " ", 2, 0, true)
                )
        );
    }

    @Test
    void failsWhenDepthOrDisplayOrderIsInvalid() {
        assertAll(
                () -> assertThrows(
                        InvalidItemCategoryException.class,
                        () -> ItemCategory.of("ENGINE", "Engine", null, 0, 0, true)
                ),
                () -> assertThrows(
                        InvalidItemCategoryException.class,
                        () -> ItemCategory.of("ENGINE", "Engine", null, 3, 0, true)
                ),
                () -> assertThrows(
                        InvalidItemCategoryException.class,
                        () -> ItemCategory.root("ENGINE", "Engine", -1, true)
                )
        );
    }
}
