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
            throw new InvalidItemException("page must be greater than or equal to 0.");
        }
        if (size <= 0) {
            throw new InvalidItemException("size must be greater than 0.");
        }
        if (totalElements < 0) {
            throw new InvalidItemException("totalElements must be greater than or equal to 0.");
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
