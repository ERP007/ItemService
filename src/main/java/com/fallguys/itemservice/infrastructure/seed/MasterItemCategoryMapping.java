package com.fallguys.itemservice.infrastructure.seed;

import java.util.List;
import java.util.Map;

final class MasterItemCategoryMapping {

    private static final MasterItemCategoryDefinition ENGINE =
            new MasterItemCategoryDefinition("ENGINE", "엔진", null, 1, 1);
    private static final MasterItemCategoryDefinition IGNITION =
            new MasterItemCategoryDefinition("IGNITION", "점화", null, 1, 2);
    private static final MasterItemCategoryDefinition BRAKE =
            new MasterItemCategoryDefinition("BRAKE", "제동", null, 1, 3);
    private static final MasterItemCategoryDefinition DRIVETRAIN =
            new MasterItemCategoryDefinition("DRIVETRAIN", "동력전달", null, 1, 4);
    private static final MasterItemCategoryDefinition SUSPENSION_STEERING =
            new MasterItemCategoryDefinition("SUSPENSION_STEERING", "현가·조향", null, 1, 5);
    private static final MasterItemCategoryDefinition ELECTRICAL =
            new MasterItemCategoryDefinition("ELECTRICAL", "전장", null, 1, 6);
    private static final MasterItemCategoryDefinition EXTERIOR_MISC =
            new MasterItemCategoryDefinition("EXTERIOR_MISC", "외장·기타", null, 1, 7);
    private static final MasterItemCategoryDefinition ENGINE_LUBRICATION =
            new MasterItemCategoryDefinition("ENGINE_LUBRICATION", "윤활계통", "ENGINE", 2, 1);
    private static final MasterItemCategoryDefinition ENGINE_FILTER =
            new MasterItemCategoryDefinition("ENGINE_FILTER", "필터", "ENGINE", 2, 2);

    private static final List<MasterItemCategoryDefinition> CATEGORIES = List.of(
            ENGINE,
            IGNITION,
            BRAKE,
            DRIVETRAIN,
            SUSPENSION_STEERING,
            ELECTRICAL,
            EXTERIOR_MISC,
            ENGINE_LUBRICATION,
            ENGINE_FILTER
    );

    private static final Map<String, MasterItemCategoryDefinition> FINAL_CATEGORY_BY_DISPLAY_PATH = Map.ofEntries(
            Map.entry("엔진/오일", ENGINE_LUBRICATION),
            Map.entry("엔진/필터", ENGINE_FILTER),
            Map.entry("점화", IGNITION),
            Map.entry("제동", BRAKE),
            Map.entry("동력전달", DRIVETRAIN),
            Map.entry("현가·조향", SUSPENSION_STEERING),
            Map.entry("전장", ELECTRICAL),
            Map.entry("외장·기타", EXTERIOR_MISC)
    );

    private MasterItemCategoryMapping() {
    }

    static List<MasterItemCategoryDefinition> allCategories() {
        return CATEGORIES;
    }

    static MasterItemCategoryDefinition requireFinalCategory(String displayPath, long recordNumber) {
        MasterItemCategoryDefinition category = FINAL_CATEGORY_BY_DISPLAY_PATH.get(displayPath);
        if (category == null) {
            throw new MasterItemSeedException("CSV 카테고리를 찾을 수 없습니다. 행=" + recordNumber + ", category=" + displayPath);
        }
        return category;
    }
}
