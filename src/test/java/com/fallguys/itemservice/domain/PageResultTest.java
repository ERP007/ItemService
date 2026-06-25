package com.fallguys.itemservice.domain;

import com.fallguys.itemservice.domain.exception.InvalidItemException;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PageResultTest {

    @Test
    void calculatesPageNavigation() {
        PageResult<String> firstPage = new PageResult<>(List.of("A", "B"), 0, 2, 5);
        PageResult<String> middlePage = new PageResult<>(List.of("C", "D"), 1, 2, 5);
        PageResult<String> lastPage = new PageResult<>(List.of("E"), 2, 2, 5);
        PageResult<String> emptyPage = new PageResult<>(List.of(), 0, 20, 0);

        assertAll(
                () -> assertEquals(3, firstPage.totalPages()),
                () -> assertTrue(firstPage.hasNext()),
                () -> assertFalse(firstPage.hasPrevious()),
                () -> assertTrue(middlePage.hasNext()),
                () -> assertTrue(middlePage.hasPrevious()),
                () -> assertFalse(lastPage.hasNext()),
                () -> assertTrue(lastPage.hasPrevious()),
                () -> assertEquals(0, emptyPage.totalPages()),
                () -> assertFalse(emptyPage.hasNext()),
                () -> assertFalse(emptyPage.hasPrevious())
        );
    }

    @Test
    void failsWhenPagingStateIsInvalid() {
        assertAll(
                () -> assertThrows(
                        NullPointerException.class,
                        () -> new PageResult<String>(null, 0, 20, 0)
                ),
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> new PageResult<>(List.of(), -1, 20, 0)
                ),
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> new PageResult<>(List.of(), 0, 0, 0)
                ),
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> new PageResult<>(List.of(), 0, 20, -1)
                )
        );
    }
}
