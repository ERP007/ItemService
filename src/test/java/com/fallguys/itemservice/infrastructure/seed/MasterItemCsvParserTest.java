package com.fallguys.itemservice.infrastructure.seed;

import com.fallguys.itemservice.domain.ItemUnit;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MasterItemCsvParserTest {

    private final MasterItemCsvParser parser = new MasterItemCsvParser();

    @Test
    void parsesValidRowsAndMapsKoreanCategoryToCode() {
        List<MasterItemCsvRow> rows = parser.parse(csv("""
                sku,name,category,unit,safety_stock,unit_price,active
                ENG-OIL-5W30-1L,엔진오일 5W-30 1L,엔진/오일,EA,50,8500,true
                BRK-PAD-FR-001,브레이크 패드 (전륜),제동,SET,25,38500,false
                """), "test.csv");

        MasterItemCsvRow first = rows.getFirst();
        MasterItemCsvRow second = rows.get(1);

        assertAll(
                () -> assertEquals(2, rows.size()),
                () -> assertEquals("ENG-OIL-5W30-1L", first.sku()),
                () -> assertEquals("ENGINE_LUBRICATION", first.categoryCode()),
                () -> assertEquals(ItemUnit.EA, first.unit()),
                () -> assertEquals(50, first.safetyStock()),
                () -> assertEquals(8500, first.unitPrice()),
                () -> assertEquals(true, first.active()),
                () -> assertEquals("BRAKE", second.categoryCode()),
                () -> assertEquals(ItemUnit.SET, second.unit()),
                () -> assertEquals(false, second.active())
        );
    }

    @Test
    void failsWhenCategoryIsUnknown() {
        MasterItemSeedException exception = assertThrows(MasterItemSeedException.class, () -> parser.parse(csv("""
                sku,name,category,unit,safety_stock,unit_price,active
                UNKNOWN-001,Unknown item,없는분류,EA,1,1000,true
                """), "test.csv"));

        assertEquals("Unknown category at row 1: 없는분류", exception.getMessage());
    }

    @Test
    void failsWhenUnitIsInvalid() {
        MasterItemSeedException exception = assertThrows(MasterItemSeedException.class, () -> parser.parse(csv("""
                sku,name,category,unit,safety_stock,unit_price,active
                UNKNOWN-001,Unknown item,제동,PACK,1,1000,true
                """), "test.csv"));

        assertEquals("Invalid unit at row 1: PACK", exception.getMessage());
    }

    @Test
    void failsWhenNumberIsNegative() {
        MasterItemSeedException exception = assertThrows(MasterItemSeedException.class, () -> parser.parse(csv("""
                sku,name,category,unit,safety_stock,unit_price,active
                UNKNOWN-001,Unknown item,제동,EA,-1,1000,true
                """), "test.csv"));

        assertEquals("Negative value at row 1: safety_stock", exception.getMessage());
    }

    @Test
    void failsWhenRequiredValueIsMissing() {
        MasterItemSeedException exception = assertThrows(MasterItemSeedException.class, () -> parser.parse(csv("""
                sku,name,category,unit,safety_stock,unit_price,active
                UNKNOWN-001,,제동,EA,1,1000,true
                """), "test.csv"));

        assertEquals("Missing required value at row 1: name", exception.getMessage());
    }

    @Test
    void failsWhenSkuIsDuplicatedInsideCsv() {
        MasterItemSeedException exception = assertThrows(MasterItemSeedException.class, () -> parser.parse(csv("""
                sku,name,category,unit,safety_stock,unit_price,active
                DUP-001,First,제동,EA,1,1000,true
                DUP-001,Second,제동,EA,2,2000,true
                """), "test.csv"));

        assertEquals("Duplicate SKU in CSV at row 2: DUP-001", exception.getMessage());
    }

    private static ByteArrayInputStream csv(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}
