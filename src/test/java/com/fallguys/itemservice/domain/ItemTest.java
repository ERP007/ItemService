package com.fallguys.itemservice.domain;

import com.fallguys.itemservice.domain.exception.InvalidItemException;
import com.fallguys.itemservice.domain.exception.InvalidItemStatusException;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemTest {

    private static final Instant CREATED_AT = Instant.parse("2026-06-07T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-06-07T01:00:00Z");

    @Test
    void createsActiveItem() {
        Item item = Item.create(" ENG-OIL-5W30-1L ", " Engine oil ", " ENGINE_OIL ", ItemUnit.EA, 50, 8500, CREATED_AT);

        assertAll(
                () -> assertEquals("ENG-OIL-5W30-1L", item.getSku()),
                () -> assertEquals("Engine oil", item.getName()),
                () -> assertEquals("ENGINE_OIL", item.getCategoryCode()),
                () -> assertEquals(ItemUnit.EA, item.getUnit()),
                () -> assertEquals(50, item.getSafetyStock()),
                () -> assertEquals(8500, item.getUnitPrice()),
                () -> assertTrue(item.isActive()),
                () -> assertEquals(CREATED_AT, item.getCreatedAt()),
                () -> assertEquals(CREATED_AT, item.getUpdatedAt())
        );
    }

    @Test
    void failsWhenRequiredValueIsMissing() {
        assertAll(
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> Item.create(" ", "Engine oil", "ENGINE_OIL", ItemUnit.EA, 50, 8500, CREATED_AT)
                ),
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> Item.create("ENG-OIL-5W30-1L", null, "ENGINE_OIL", ItemUnit.EA, 50, 8500, CREATED_AT)
                ),
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> Item.create("ENG-OIL-5W30-1L", "Engine oil", "", ItemUnit.EA, 50, 8500, CREATED_AT)
                ),
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> Item.create("ENG-OIL-5W30-1L", "Engine oil", "ENGINE_OIL", null, 50, 8500, CREATED_AT)
                ),
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> Item.create("ENG-OIL-5W30-1L", "Engine oil", "ENGINE_OIL", ItemUnit.EA, 50, 8500, null)
                ),
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> Item.of("ENG-OIL-5W30-1L", "Engine oil", "ENGINE_OIL", ItemUnit.EA, 50, 8500, true, CREATED_AT, null)
                )
        );
    }

    @Test
    void failsWhenSafetyStockIsNegative() {
        assertThrows(
                InvalidItemException.class,
                () -> Item.create("ENG-OIL-5W30-1L", "Engine oil", "ENGINE_OIL", ItemUnit.EA, -1, 8500, CREATED_AT)
        );
    }

    @Test
    void failsWhenUnitPriceIsNegative() {
        assertThrows(
                InvalidItemException.class,
                () -> Item.create("ENG-OIL-5W30-1L", "Engine oil", "ENGINE_OIL", ItemUnit.EA, 50, -1, CREATED_AT)
        );
    }

    @Test
    void updatesEditableFieldsAndUpdatedAt() {
        Item item = Item.create("ENG-OIL-5W30-1L", "Engine oil", "ENGINE_OIL", ItemUnit.EA, 50, 8500, CREATED_AT);

        item.update(" Oil filter ", " ENGINE_FILTER ", ItemUnit.SET, 10, 12000, UPDATED_AT);

        assertAll(
                () -> assertEquals("ENG-OIL-5W30-1L", item.getSku()),
                () -> assertEquals("Oil filter", item.getName()),
                () -> assertEquals("ENGINE_FILTER", item.getCategoryCode()),
                () -> assertEquals(ItemUnit.SET, item.getUnit()),
                () -> assertEquals(10, item.getSafetyStock()),
                () -> assertEquals(12000, item.getUnitPrice()),
                () -> assertEquals(CREATED_AT, item.getCreatedAt()),
                () -> assertEquals(UPDATED_AT, item.getUpdatedAt())
        );
    }

    @Test
    void activatesAndDeactivatesItem() {
        Item item = Item.create("ENG-OIL-5W30-1L", "Engine oil", "ENGINE_OIL", ItemUnit.EA, 50, 8500, CREATED_AT);

        item.deactivate(UPDATED_AT);

        assertAll(
                () -> assertFalse(item.isActive()),
                () -> assertEquals(UPDATED_AT, item.getUpdatedAt())
        );

        Instant activatedAt = Instant.parse("2026-06-07T02:00:00Z");
        item.activate(activatedAt);

        assertAll(
                () -> assertTrue(item.isActive()),
                () -> assertEquals(activatedAt, item.getUpdatedAt())
        );
    }

    @Test
    void failsWhenActivatingAlreadyActiveItem() {
        Item item = Item.create("ENG-OIL-5W30-1L", "Engine oil", "ENGINE_OIL", ItemUnit.EA, 50, 8500, CREATED_AT);

        InvalidItemStatusException exception = assertThrows(
                InvalidItemStatusException.class,
                () -> item.activate(UPDATED_AT)
        );

        assertAll(
                () -> assertEquals("ITM-017", exception.getCode()),
                () -> assertTrue(item.isActive()),
                () -> assertEquals(CREATED_AT, item.getUpdatedAt())
        );
    }

    @Test
    void failsWhenDeactivatingAlreadyInactiveItem() {
        Item item = Item.of(
                "ENG-OIL-5W30-1L",
                "Engine oil",
                "ENGINE_OIL",
                ItemUnit.EA,
                50,
                8500,
                false,
                CREATED_AT,
                CREATED_AT
        );

        InvalidItemStatusException exception = assertThrows(
                InvalidItemStatusException.class,
                () -> item.deactivate(UPDATED_AT)
        );

        assertAll(
                () -> assertEquals("ITM-017", exception.getCode()),
                () -> assertFalse(item.isActive()),
                () -> assertEquals(CREATED_AT, item.getUpdatedAt())
        );
    }
}
