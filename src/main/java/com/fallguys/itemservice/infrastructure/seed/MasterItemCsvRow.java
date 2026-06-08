package com.fallguys.itemservice.infrastructure.seed;

import com.fallguys.itemservice.domain.Item;
import com.fallguys.itemservice.domain.ItemUnit;

import java.time.Instant;

record MasterItemCsvRow(
        String sku,
        String name,
        String categoryCode,
        ItemUnit unit,
        int safetyStock,
        int unitPrice,
        boolean active
) {

    Item toItem(Instant timestamp) {
        return Item.of(sku, name, categoryCode, unit, safetyStock, unitPrice, active, timestamp, timestamp);
    }
}
