package com.fallguys.itemservice.infrastructure.seed;

public record MasterItemSeedResult(
        int categoriesCreated,
        int categoriesSkipped,
        int itemsCreated,
        int itemsSkipped
) {
}
