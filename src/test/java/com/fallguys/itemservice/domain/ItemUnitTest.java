package com.fallguys.itemservice.domain;

import com.fallguys.itemservice.domain.exception.InvalidItemUnitException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ItemUnitTest {

    @Test
    void convertsSupportedUnits() {
        assertAll(
                () -> assertEquals(ItemUnit.EA, ItemUnit.from("EA")),
                () -> assertEquals(ItemUnit.BOX, ItemUnit.from(" box ")),
                () -> assertEquals(ItemUnit.SET, ItemUnit.from(" set ")),
                () -> assertEquals(ItemUnit.L, ItemUnit.from("l"))
        );
    }

    @Test
    void failsWhenUnitIsUnsupported() {
        assertAll(
                () -> assertThrows(InvalidItemUnitException.class, () -> ItemUnit.from("PACK")),
                () -> assertThrows(InvalidItemUnitException.class, () -> ItemUnit.from(" "))
        );
    }
}
