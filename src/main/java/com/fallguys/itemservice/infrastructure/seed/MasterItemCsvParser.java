package com.fallguys.itemservice.infrastructure.seed;

import com.fallguys.itemservice.domain.ItemUnit;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

class MasterItemCsvParser {

    private static final List<String> EXPECTED_HEADERS = List.of(
            "sku",
            "name",
            "category",
            "unit",
            "safety_stock",
            "unit_price",
            "active"
    );

    List<MasterItemCsvRow> parse(InputStream inputStream, String sourceName) {
        CSVFormat format = CSVFormat.DEFAULT.builder()
                .setHeader()
                .setSkipHeaderRecord(true)
                .setTrim(true)
                .get();

        try (
                Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                CSVParser csvParser = format.parse(reader)
        ) {
            validateHeaders(csvParser, sourceName);

            List<MasterItemCsvRow> rows = new ArrayList<>();
            Set<String> seenSkus = new HashSet<>();
            for (CSVRecord record : csvParser) {
                MasterItemCsvRow row = parseRecord(record);
                if (!seenSkus.add(row.sku())) {
                    throw new MasterItemSeedException("Duplicate SKU in CSV at row " + record.getRecordNumber() + ": " + row.sku());
                }
                rows.add(row);
            }
            return List.copyOf(rows);
        } catch (IOException ex) {
            throw new MasterItemSeedException("Failed to parse item master seed CSV: " + sourceName, ex);
        }
    }

    private static void validateHeaders(CSVParser csvParser, String sourceName) {
        List<String> actualHeaders = new ArrayList<>(csvParser.getHeaderMap().keySet());
        if (!EXPECTED_HEADERS.equals(actualHeaders)) {
            throw new MasterItemSeedException("Invalid item master seed CSV headers in " + sourceName + ": " + actualHeaders);
        }
    }

    private static MasterItemCsvRow parseRecord(CSVRecord record) {
        long recordNumber = record.getRecordNumber();
        String sku = requireText(record, "sku");
        String name = requireText(record, "name");
        String categoryDisplayPath = requireText(record, "category");
        String categoryCode = MasterItemCategoryMapping.requireFinalCategory(categoryDisplayPath, recordNumber)
                .finalCategoryCode();

        return new MasterItemCsvRow(
                sku,
                name,
                categoryCode,
                parseUnit(requireText(record, "unit"), recordNumber),
                parseNonNegativeInt(requireText(record, "safety_stock"), "safety_stock", recordNumber),
                parseNonNegativeInt(requireText(record, "unit_price"), "unit_price", recordNumber),
                parseBoolean(requireText(record, "active"), recordNumber)
        );
    }

    private static String requireText(CSVRecord record, String header) {
        String value = record.get(header);
        if (value == null || value.isBlank()) {
            throw new MasterItemSeedException("Missing required value at row " + record.getRecordNumber() + ": " + header);
        }
        return value.trim();
    }

    private static ItemUnit parseUnit(String unit, long recordNumber) {
        try {
            return ItemUnit.from(unit);
        } catch (RuntimeException ex) {
            throw new MasterItemSeedException("Invalid unit at row " + recordNumber + ": " + unit, ex);
        }
    }

    private static int parseNonNegativeInt(String value, String header, long recordNumber) {
        try {
            int parsed = Integer.parseInt(value);
            if (parsed < 0) {
                throw new MasterItemSeedException("Negative value at row " + recordNumber + ": " + header);
            }
            return parsed;
        } catch (NumberFormatException ex) {
            throw new MasterItemSeedException("Invalid number at row " + recordNumber + ": " + header + "=" + value, ex);
        }
    }

    private static boolean parseBoolean(String value, long recordNumber) {
        if ("true".equalsIgnoreCase(value)) {
            return true;
        }
        if ("false".equalsIgnoreCase(value)) {
            return false;
        }
        throw new MasterItemSeedException("Invalid active value at row " + recordNumber + ": " + value);
    }
}
