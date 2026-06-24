package com.fallguys.itemservice.domain;

import java.util.regex.Pattern;

public final class ItemSkuPolicy {

    public static final int MAX_LENGTH = 64;
    private static final Pattern SKU_PATTERN = Pattern.compile("^[A-Z0-9][A-Z0-9_-]{0,63}$");

    private ItemSkuPolicy() {
    }

    public static String normalize(String sku) {
        if (sku == null || sku.isBlank()) {
            return null;
        }
        return sku.trim();
    }

    public static boolean isValid(String sku) {
        return sku != null && SKU_PATTERN.matcher(sku).matches();
    }
}
