package com.fallguys.itemservice.infrastructure.seed;

import com.fallguys.itemservice.domain.ItemSkuPolicy;
import com.fallguys.itemservice.domain.ItemUnit;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MasterItemSeedCsvContractTest {

    private static final int MASTER_ITEM_COUNT = 10_000;
    private static final int ACTIVE_ITEM_COUNT = 9_600;
    private static final int INACTIVE_ITEM_COUNT = 400;

    private static final List<String> BASE_SKUS = List.of(
            "ENG-OIL-5W30-1L",
            "ENG-OIL-5W30-4L",
            "ENG-OIL-0W20-4L",
            "ENG-OIL-15W40-4L",
            "MIS-OIL-ATF-4L",
            "GEAR-OIL-75W90-1L",
            "OIL-FLT-001",
            "AIR-FLT-002",
            "CAB-FLT-003",
            "FUEL-FLT-004",
            "SPK-PLG-IRD-04",
            "SPK-PLG-PLT-04",
            "IGN-COIL-V6-01",
            "PLG-CBL-SET-01",
            "BRK-PAD-FR-001",
            "BRK-PAD-RR-001",
            "BRK-DSC-FR-001",
            "BRK-DSC-RR-001",
            "BRK-CAL-FR-001",
            "BRK-FLU-DOT4-1L",
            "CLT-DSK-MED-01",
            "CLT-CVR-MED-01",
            "TIM-BLT-V6-02",
            "CV-JNT-FR-01",
            "DRV-SHF-RR-01",
            "SHK-ABS-FR-11",
            "SHK-ABS-RR-11",
            "SPR-COL-FR-01",
            "TIE-ROD-LH-01",
            "STB-LNK-FR-01",
            "BAT-12V-60",
            "BAT-12V-80",
            "HLT-LED-LH-01",
            "TLT-LED-LH-01",
            "RLY-12V-30A-01",
            "O2-SNS-UP-01",
            "WPR-BLD-24",
            "WPR-BLD-20",
            "WPR-BLD-22",
            "TIR-205-55-16",
            "TIR-225-45-17",
            "WSH-FLU-2L"
    );

    private final MasterItemCsvParser parser = new MasterItemCsvParser();

    @Test
    void classpathCsvContainsTenThousandValidUniqueItemsAndPreservesBaseRows() throws IOException {
        List<MasterItemCsvRow> rows = parseClasspathCsv();
        Set<String> uniqueSkus = rows.stream()
                .map(MasterItemCsvRow::sku)
                .collect(Collectors.toCollection(HashSet::new));
        Set<String> uniqueNames = rows.stream()
                .map(MasterItemCsvRow::name)
                .collect(Collectors.toCollection(HashSet::new));
        Map<String, Long> categoryCounts = rows.stream()
                .collect(Collectors.groupingBy(MasterItemCsvRow::categoryCode, Collectors.counting()));
        Set<ItemUnit> units = rows.stream()
                .map(MasterItemCsvRow::unit)
                .collect(Collectors.toSet());
        long activeCount = rows.stream().filter(MasterItemCsvRow::active).count();
        long inactiveCount = rows.stream().filter(row -> !row.active()).count();

        assertAll(
                () -> assertEquals(MASTER_ITEM_COUNT, rows.size()),
                () -> assertEquals(BASE_SKUS, rows.subList(0, BASE_SKUS.size()).stream()
                        .map(MasterItemCsvRow::sku)
                        .toList()),
                () -> assertEquals(MASTER_ITEM_COUNT, uniqueSkus.size()),
                () -> assertEquals(MASTER_ITEM_COUNT, uniqueNames.size()),
                () -> assertTrue(rows.stream().allMatch(row -> ItemSkuPolicy.isValid(row.sku()))),
                () -> assertTrue(rows.stream().allMatch(row -> row.sku().length() <= 50)),
                () -> assertEquals(Set.of(ItemUnit.EA, ItemUnit.BOX, ItemUnit.SET, ItemUnit.L), units),
                () -> assertEquals(ACTIVE_ITEM_COUNT, activeCount),
                () -> assertEquals(INACTIVE_ITEM_COUNT, inactiveCount),
                () -> assertEquals(1_200L, categoryCounts.get("ENGINE_LUBRICATION")),
                () -> assertEquals(1_100L, categoryCounts.get("ENGINE_FILTER")),
                () -> assertEquals(900L, categoryCounts.get("IGNITION")),
                () -> assertEquals(1_400L, categoryCounts.get("BRAKE")),
                () -> assertEquals(1_200L, categoryCounts.get("DRIVETRAIN")),
                () -> assertEquals(1_400L, categoryCounts.get("SUSPENSION_STEERING")),
                () -> assertEquals(1_500L, categoryCounts.get("ELECTRICAL")),
                () -> assertEquals(1_300L, categoryCounts.get("EXTERIOR_MISC"))
        );
    }

    private List<MasterItemCsvRow> parseClasspathCsv() throws IOException {
        ClassPathResource resource = new ClassPathResource("data/items.csv");
        try (InputStream inputStream = resource.getInputStream()) {
            return parser.parse(inputStream, resource.getDescription());
        }
    }
}
