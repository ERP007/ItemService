package com.fallguys.itemservice.domain;

import com.fallguys.itemservice.domain.exception.InvalidItemCategoryException;

import java.util.Objects;

public class ItemCategory {

    public static final int ROOT_DEPTH = 1;
    public static final int SUB_CATEGORY_DEPTH = 2;

    private final String code;
    private final String name;
    private final String parentCode;
    private final int depth;
    private final int displayOrder;
    private final boolean active;

    private ItemCategory(String code, String name, String parentCode, int depth, int displayOrder, boolean active) {
        this.code = requireText(code, "code");
        this.name = requireText(name, "name");
        this.depth = requireDepth(depth);
        this.parentCode = requireParentCode(parentCode, this.depth);
        this.displayOrder = requireNonNegative(displayOrder, "displayOrder");
        this.active = active;
    }

    public static ItemCategory root(String code, String name, int displayOrder, boolean active) {
        return new ItemCategory(code, name, null, ROOT_DEPTH, displayOrder, active);
    }

    public static ItemCategory subCategory(String code, String name, String parentCode, int displayOrder, boolean active) {
        return new ItemCategory(code, name, parentCode, SUB_CATEGORY_DEPTH, displayOrder, active);
    }

    public static ItemCategory of(String code, String name, String parentCode, int depth, int displayOrder, boolean active) {
        return new ItemCategory(code, name, parentCode, depth, displayOrder, active);
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public String getParentCode() {
        return parentCode;
    }

    public int getDepth() {
        return depth;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public boolean isActive() {
        return active;
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidItemCategoryException("필수값이 누락되었습니다: " + fieldName);
        }
        return value.trim();
    }

    private static int requireDepth(int depth) {
        if (depth != ROOT_DEPTH && depth != SUB_CATEGORY_DEPTH) {
            throw new InvalidItemCategoryException("depth는 1 또는 2여야 합니다.");
        }
        return depth;
    }

    private static String requireParentCode(String parentCode, int depth) {
        String normalizedParentCode = trimToNull(parentCode);

        if (depth == ROOT_DEPTH && normalizedParentCode != null) {
            throw new InvalidItemCategoryException("대분류에는 parentCode를 지정할 수 없습니다.");
        }
        if (depth == SUB_CATEGORY_DEPTH && normalizedParentCode == null) {
            throw new InvalidItemCategoryException("중분류에는 parentCode가 필요합니다.");
        }
        return normalizedParentCode;
    }

    private static String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static int requireNonNegative(int value, String fieldName) {
        if (value < 0) {
            throw new InvalidItemCategoryException(fieldName + "은(는) 0 이상이어야 합니다.");
        }
        return value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ItemCategory that)) {
            return false;
        }
        return code.equals(that.code);
    }

    @Override
    public int hashCode() {
        return Objects.hash(code);
    }
}
