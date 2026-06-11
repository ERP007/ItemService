package com.fallguys.itemservice.domain;

import com.fallguys.itemservice.domain.exception.InvalidItemException;

public record SearchItemsQuery(
        String search,
        String categoryCode,
        Boolean active,
        int page,
        int size,
        ItemSortBy sortBy,
        SortDirection sortDirection
) {

    public SearchItemsQuery {
        search = trimToNull(search);
        categoryCode = trimToNull(categoryCode);
        if (page < 0) {
            throw new InvalidItemException("page는 0 이상이어야 합니다.");
        }
        if (size <= 0) {
            throw new InvalidItemException("size는 1 이상이어야 합니다.");
        }
        sortBy = sortBy == null ? ItemSortBy.NAME : sortBy;
        sortDirection = sortDirection == null ? SortDirection.ASC : sortDirection;
    }

    public static SearchItemsQuery defaultQuery() {
        return new SearchItemsQuery(null, null, null, 0, 20, ItemSortBy.NAME, SortDirection.ASC);
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
