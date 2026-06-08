package com.fallguys.itemservice.domain;

import com.fallguys.itemservice.domain.exception.InvalidItemUnitException;

import java.util.Locale;

public enum ItemUnit {
    EA("EA", "EA"),
    BOX("BOX", "BOX"),
    SET("SET", "SET"),
    L("L", "L");

    private final String code;
    private final String label;

    ItemUnit(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public static ItemUnit from(String value) {
        if (value == null || value.isBlank()) {
            throw new InvalidItemUnitException("unit is required.");
        }

        String normalizedValue = value.trim().toUpperCase(Locale.ROOT);
        for (ItemUnit unit : values()) {
            if (unit.code.equals(normalizedValue)) {
                return unit;
            }
        }
        throw new InvalidItemUnitException("Unsupported item unit: " + value.trim());
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }
}
