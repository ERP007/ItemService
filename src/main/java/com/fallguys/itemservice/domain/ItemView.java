package com.fallguys.itemservice.domain;

import java.time.Instant;

public record ItemView(
        String sku,
        String name,
        String categoryCode,
        String categoryName,
        String parentCategoryCode,
        String parentCategoryName,
        ItemUnit unit,
        int safetyStock,
        int unitPrice,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
