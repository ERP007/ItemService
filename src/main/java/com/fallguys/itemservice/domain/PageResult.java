package com.fallguys.itemservice.domain;

import com.fallguys.itemservice.domain.exception.InvalidItemException;

import java.util.List;
import java.util.Objects;

public record PageResult<T>(
        List<T> content,
        int page,
        int size,
        long totalElements
) {

    public PageResult {
        content = List.copyOf(Objects.requireNonNull(content, "content"));
        if (page < 0) {
            throw new InvalidItemException("page는 0 이상이어야 합니다.");
        }
        if (size <= 0) {
            throw new InvalidItemException("size는 1 이상이어야 합니다.");
        }
        if (totalElements < 0) {
            throw new InvalidItemException("totalElements는 0 이상이어야 합니다.");
        }
    }

    public int totalPages() {
        return (int) Math.ceil((double) totalElements / size);
    }

    public boolean hasNext() {
        return page + 1 < totalPages();
    }

    public boolean hasPrevious() {
        return page > 0;
    }
}
