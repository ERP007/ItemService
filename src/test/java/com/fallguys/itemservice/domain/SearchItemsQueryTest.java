package com.fallguys.itemservice.domain;

import com.fallguys.itemservice.domain.exception.InvalidItemException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SearchItemsQueryTest {

    @Test
    void normalizesSearchConditionsAndDefaultsSort() {
        SearchItemsQuery query = new SearchItemsQuery(
                " oil ",
                " ENGINE_OIL ",
                true,
                0,
                20,
                null,
                null
        );

        assertAll(
                () -> assertEquals("oil", query.search()),
                () -> assertEquals("ENGINE_OIL", query.categoryCode()),
                () -> assertEquals(true, query.active()),
                () -> assertEquals(0, query.page()),
                () -> assertEquals(20, query.size()),
                () -> assertEquals(ItemSortBy.NAME, query.sortBy()),
                () -> assertEquals(SortDirection.ASC, query.sortDirection())
        );
    }

    @Test
    void convertsBlankSearchConditionsToNull() {
        SearchItemsQuery query = new SearchItemsQuery(" ", " ", null, 0, 20, ItemSortBy.SKU, SortDirection.DESC);

        assertAll(
                () -> assertNull(query.search()),
                () -> assertNull(query.categoryCode()),
                () -> assertNull(query.active()),
                () -> assertEquals(ItemSortBy.SKU, query.sortBy()),
                () -> assertEquals(SortDirection.DESC, query.sortDirection())
        );
    }

    @Test
    void createsDefaultQuery() {
        SearchItemsQuery query = SearchItemsQuery.defaultQuery();

        assertAll(
                () -> assertNull(query.search()),
                () -> assertNull(query.categoryCode()),
                () -> assertNull(query.active()),
                () -> assertEquals(0, query.page()),
                () -> assertEquals(20, query.size()),
                () -> assertEquals(ItemSortBy.NAME, query.sortBy()),
                () -> assertEquals(SortDirection.ASC, query.sortDirection())
        );
    }

    @Test
    void failsWhenPageOrSizeIsInvalid() {
        assertAll(
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> new SearchItemsQuery(null, null, null, -1, 20, ItemSortBy.NAME, SortDirection.ASC)
                ),
                () -> assertThrows(
                        InvalidItemException.class,
                        () -> new SearchItemsQuery(null, null, null, 0, 0, ItemSortBy.NAME, SortDirection.ASC)
                )
        );
    }
}
