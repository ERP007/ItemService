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

        assertEquals("CSV 카테고리를 찾을 수 없습니다. 행=1, category=없는분류", exception.getMessage());
    }

    @Test
    void failsWhenUnitIsInvalid() {
        MasterItemSeedException exception = assertThrows(MasterItemSeedException.class, () -> parser.parse(csv("""
                sku,name,category,unit,safety_stock,unit_price,active
                UNKNOWN-001,Unknown item,제동,PACK,1,1000,true
                """), "test.csv"));

        assertEquals("CSV 단위가 올바르지 않습니다. 행=1, unit=PACK", exception.getMessage());
    }

    @Test
    void failsWhenNumberIsNegative() {
        MasterItemSeedException exception = assertThrows(MasterItemSeedException.class, () -> parser.parse(csv("""
                sku,name,category,unit,safety_stock,unit_price,active
                UNKNOWN-001,Unknown item,제동,EA,-1,1000,true
                """), "test.csv"));

        assertEquals("CSV 숫자 값은 0 이상이어야 합니다. 행=1, 컬럼=safety_stock", exception.getMessage());
    }

    @Test
    void failsWhenRequiredValueIsMissing() {
        MasterItemSeedException exception = assertThrows(MasterItemSeedException.class, () -> parser.parse(csv("""
                sku,name,category,unit,safety_stock,unit_price,active
                UNKNOWN-001,,제동,EA,1,1000,true
                """), "test.csv"));

        assertEquals("CSV 필수값이 누락되었습니다. 행=1, 컬럼=name", exception.getMessage());
    }

    @Test
    void failsWhenSkuIsDuplicatedInsideCsv() {
        MasterItemSeedException exception = assertThrows(MasterItemSeedException.class, () -> parser.parse(csv("""
                sku,name,category,unit,safety_stock,unit_price,active
                DUP-001,First,제동,EA,1,1000,true
                DUP-001,Second,제동,EA,2,2000,true
                """), "test.csv"));

        assertEquals("CSV에 중복된 SKU가 있습니다. 행=2, sku=DUP-001", exception.getMessage());
    }

    private static ByteArrayInputStream csv(String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }
}
