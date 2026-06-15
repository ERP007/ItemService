package com.fallguys.itemservice.controller.dto;

import com.fallguys.itemservice.domain.ItemUnit;
import com.fallguys.itemservice.domain.exception.InvalidItemRequestException;
import com.fallguys.itemservice.domain.exception.ItemErrorCode;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.regex.Pattern;

public final class ItemRequestValidator {

    private static final Pattern SKU_PATTERN = Pattern.compile("^[A-Z0-9][A-Z0-9_-]{0,63}$");
    private static final Pattern CATEGORY_CODE_PATTERN = Pattern.compile("^[A-Z0-9][A-Z0-9_]{0,63}$");

    private ItemRequestValidator() {
    }

    public static String requireSku(String sku, ItemErrorCode missingCode) {
        String normalizedSku = trimToNull(sku);
        if (normalizedSku == null) {
            throw new InvalidItemRequestException(missingCode, "SKU는 필수입니다.");
        }
        if (!SKU_PATTERN.matcher(normalizedSku).matches()) {
            throw new InvalidItemRequestException(ItemErrorCode.INVALID_SKU_FORMAT, "SKU 형식이 올바르지 않습니다: " + normalizedSku);
        }
        return normalizedSku;
    }

    public static List<String> requireSkus(List<String> skus, int maxSize) {
        if (skus == null || skus.isEmpty()) {
            throw new InvalidItemRequestException(ItemErrorCode.SKUS_REQUIRED, "SKU 목록은 필수입니다.");
        }

        LinkedHashSet<String> normalizedSkus = new LinkedHashSet<>();
        for (String sku : skus) {
            normalizedSkus.add(requireSku(sku, ItemErrorCode.INVALID_SKU_FORMAT));
        }
        if (normalizedSkus.size() > maxSize) {
            throw new InvalidItemRequestException(ItemErrorCode.TOO_MANY_SKUS, "조회할 SKU는 최대 " + maxSize + "개까지 가능합니다.");
        }
        return List.copyOf(normalizedSkus);
    }

    public static String requireNameForCreate(String name) {
        String normalizedName = trimToNull(name);
        if (normalizedName == null) {
            throw new InvalidItemRequestException(ItemErrorCode.ITEM_NAME_REQUIRED, "부품명은 필수입니다.");
        }
        return normalizedName;
    }

    public static String requireNameForUpdate(String name) {
        String normalizedName = trimToNull(name);
        if (normalizedName == null) {
            throw new InvalidItemRequestException(ItemErrorCode.INVALID_ITEM_NAME, "부품명이 올바르지 않습니다.");
        }
        return normalizedName;
    }

    public static String requireCategoryForCreate(String categoryCode) {
        String normalizedCategoryCode = trimToNull(categoryCode);
        if (normalizedCategoryCode == null) {
            throw new InvalidItemRequestException(ItemErrorCode.CATEGORY_REQUIRED, "카테고리는 필수입니다.");
        }
        return requireCategoryFormat(normalizedCategoryCode, ItemErrorCode.INVALID_CATEGORY_CODE);
    }

    public static String requireCategoryForUpdate(String categoryCode) {
        return requireCategoryFormat(categoryCode, ItemErrorCode.INVALID_CATEGORY_CODE);
    }

    public static String requireSubCategoryForUpdate(String subCategoryCode) {
        return requireCategoryFormat(subCategoryCode, ItemErrorCode.INVALID_CATEGORY_CODE);
    }

    public static String requireCategoryForFilter(String categoryCode) {
        return requireCategoryFormat(categoryCode, ItemErrorCode.INVALID_CATEGORY_CODE);
    }

    public static ItemUnit requireUnit(String unit) {
        try {
            return ItemUnit.from(unit);
        } catch (RuntimeException ex) {
            throw new InvalidItemRequestException(ItemErrorCode.INVALID_UNIT, "단위가 올바르지 않습니다: " + unit);
        }
    }

    public static int requireSafetyStock(Integer safetyStock) {
        if (safetyStock == null || safetyStock < 0) {
            throw new InvalidItemRequestException(ItemErrorCode.INVALID_SAFETY_STOCK, "안전재고는 0 이상이어야 합니다.");
        }
        return safetyStock;
    }

    public static int requireUnitPrice(Integer unitPrice) {
        if (unitPrice == null || unitPrice < 0) {
            throw new InvalidItemRequestException(ItemErrorCode.INVALID_UNIT_PRICE, "기준 단가는 0 이상이어야 합니다.");
        }
        return unitPrice;
    }

    public static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static String requireCategoryFormat(String categoryCode, ItemErrorCode errorCode) {
        String normalizedCategoryCode = trimToNull(categoryCode);
        if (normalizedCategoryCode == null || !CATEGORY_CODE_PATTERN.matcher(normalizedCategoryCode).matches()) {
            throw new InvalidItemRequestException(errorCode, "카테고리 코드 형식이 올바르지 않습니다: " + categoryCode);
        }
        return normalizedCategoryCode;
    }
}
