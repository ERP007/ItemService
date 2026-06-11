package com.fallguys.itemservice.domain;

import com.fallguys.itemservice.domain.exception.InvalidItemException;

import java.util.List;
import java.util.Objects;

public record SearchItemViewsQuery(
        String search,
        List<String> categoryCodes,
        Boolean active,
        int page,
        int size,
        ItemSortBy sortBy,
        SortDirection sortDirection
) {

    public SearchItemViewsQuery {
        search = trimToNull(search);
        categoryCodes = List.copyOf(Objects.requireNonNull(categoryCodes, "categoryCodes"));
        if (page < 0) {
            throw new InvalidItemException("page는 0 이상이어야 합니다.");
        }
        if (size <= 0) {
            throw new InvalidItemException("size는 1 이상이어야 합니다.");
        }
        sortBy = sortBy == null ? ItemSortBy.UPDATED_AT : sortBy;
        sortDirection = sortDirection == null ? SortDirection.DESC : sortDirection;
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
