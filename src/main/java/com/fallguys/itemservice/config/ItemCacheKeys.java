package com.fallguys.itemservice.config;

public final class ItemCacheKeys {

    private static final String ITEM_DETAIL_PREFIX = "item:detail:";

    private ItemCacheKeys() {
    }

    public static String detail(String sku) {
        return ITEM_DETAIL_PREFIX + normalize(sku);
    }

    private static String normalize(String sku) {
        return sku == null ? "" : sku.trim();
    }
}
