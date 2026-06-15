package com.fallguys.itemservice.domain;

import com.fallguys.itemservice.domain.exception.DuplicateItemSkuException;
import com.fallguys.itemservice.domain.exception.InactiveItemCannotBeModifiedException;
import com.fallguys.itemservice.domain.exception.InvalidItemStatusException;
import com.fallguys.itemservice.domain.exception.ItemNotFoundException;
import com.fallguys.itemservice.domain.exception.UnavailableItemCategoryException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ItemServiceTest {

    private static final Instant NOW = Instant.parse("2026-06-07T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);

    private FakeItemRepository itemRepository;
    private FakeItemCategoryRepository itemCategoryRepository;
    private ItemService itemService;

    @BeforeEach
    void setUp() {
        itemRepository = new FakeItemRepository();
        itemCategoryRepository = new FakeItemCategoryRepository();
        itemService = new ItemService(itemRepository, itemCategoryRepository, CLOCK);
    }

    @Test
    void createsItem() {
        itemCategoryRepository.addActiveCategory("ENGINE_OIL");

        Item item = itemService.create(new CreateItemCommand(
                " ENG-OIL-5W30-1L ",
                " Engine oil ",
                " ENGINE_OIL ",
                ItemUnit.EA,
                50,
                8500
        ));

        assertAll(
                () -> assertEquals("ENG-OIL-5W30-1L", item.getSku()),
                () -> assertEquals("Engine oil", item.getName()),
                () -> assertEquals("ENGINE_OIL", item.getCategoryCode()),
                () -> assertEquals(ItemUnit.EA, item.getUnit()),
                () -> assertEquals(50, item.getSafetyStock()),
                () -> assertEquals(8500, item.getUnitPrice()),
                () -> assertTrue(item.isActive()),
                () -> assertEquals(NOW, item.getCreatedAt()),
                () -> assertEquals(NOW, item.getUpdatedAt()),
                () -> assertTrue(itemRepository.existsBySku("ENG-OIL-5W30-1L"))
        );
    }

    @Test
    void failsWhenSkuAlreadyExists() {
        itemRepository.save(existingItem("ENG-OIL-5W30-1L", "ENGINE_OIL", true));

        assertThrows(
                DuplicateItemSkuException.class,
                () -> itemService.create(new CreateItemCommand(
                        "ENG-OIL-5W30-1L",
                        "Engine oil",
                        "ENGINE_OIL",
                        ItemUnit.EA,
                        50,
                        8500
                ))
        );
    }

    @Test
    void failsWhenCategoryIsUnavailableForCreate() {
        UnavailableItemCategoryException exception = assertThrows(
                UnavailableItemCategoryException.class,
                () -> itemService.create(new CreateItemCommand(
                        "ENG-OIL-5W30-1L",
                        "Engine oil",
                        "ENGINE_OIL",
                        ItemUnit.EA,
                        50,
                        8500
                ))
        );

        assertAll(
                () -> assertEquals("ITM-003", exception.getCode()),
                () -> assertFalse(itemRepository.existsBySku("ENG-OIL-5W30-1L"))
        );
    }

    @Test
    void getsItemBySku() {
        Item expected = existingItem("ENG-OIL-5W30-1L", "ENGINE_OIL", true);
        itemRepository.save(expected);

        Item found = itemService.getBySku(" ENG-OIL-5W30-1L ");

        assertSame(expected, found);
    }

    @Test
    void getsItemViewBySku() {
        itemRepository.save(existingItem("ENG-OIL-5W30-1L", "ENGINE_OIL", true));

        ItemView found = itemService.getViewBySku(" ENG-OIL-5W30-1L ");

        assertAll(
                () -> assertEquals("ENG-OIL-5W30-1L", found.sku()),
                () -> assertEquals("ENGINE_OIL", found.categoryCode()),
                () -> assertEquals("Category", found.categoryName()),
                () -> assertEquals("ENGINE", found.parentCategoryCode())
        );
    }

    @Test
    void getsItemsBySkus() {
        itemRepository.save(existingItem("ENG-OIL-5W30-1L", "ENGINE_OIL", true));
        itemRepository.save(existingItem("ENG-FILTER-A", "ENGINE_FILTER", true));

        List<Item> found = itemService.getBySkus(List.of("UNKNOWN", "ENG-FILTER-A", "ENG-OIL-5W30-1L"));
        List<String> foundSkus = found.stream()
                .map(Item::getSku)
                .toList();

        assertEquals(List.of("ENG-FILTER-A", "ENG-OIL-5W30-1L"), foundSkus);
    }

    @Test
    void returnsEmptyWhenBatchSkuListIsEmpty() {
        assertTrue(itemService.getBySkus(List.of()).isEmpty());
    }

    @Test
    void failsWhenItemDoesNotExist() {
        assertAll(
                () -> assertThrows(ItemNotFoundException.class, () -> itemService.getBySku("UNKNOWN")),
                () -> assertThrows(ItemNotFoundException.class, () -> itemService.getViewBySku("UNKNOWN"))
        );
    }

    @Test
    void checksSkuAvailability() {
        itemRepository.save(existingItem("ENG-OIL-5W30-1L", "ENGINE_OIL", true));

        assertAll(
                () -> assertFalse(itemService.isSkuAvailable("ENG-OIL-5W30-1L")),
                () -> assertTrue(itemService.isSkuAvailable("ENG-OIL-0W20-4L"))
        );
    }

    @Test
    void updatesItem() {
        itemRepository.save(existingItem("ENG-OIL-5W30-1L", "ENGINE_OIL", true));
        itemCategoryRepository.addActiveCategory("ENGINE_FILTER");

        Item updated = itemService.update(new UpdateItemCommand(
                "ENG-OIL-5W30-1L",
                "Oil filter",
                "ENGINE_FILTER",
                ItemUnit.SET,
                10,
                12000
        ));

        assertAll(
                () -> assertEquals("ENG-OIL-5W30-1L", updated.getSku()),
                () -> assertEquals("Oil filter", updated.getName()),
                () -> assertEquals("ENGINE_FILTER", updated.getCategoryCode()),
                () -> assertEquals(ItemUnit.SET, updated.getUnit()),
                () -> assertEquals(10, updated.getSafetyStock()),
                () -> assertEquals(12000, updated.getUnitPrice()),
                () -> assertEquals(NOW, updated.getUpdatedAt())
        );
    }

    @Test
    void failsWhenCategoryIsUnavailableForUpdate() {
        itemRepository.save(existingItem("ENG-OIL-5W30-1L", "ENGINE_OIL", true));

        UnavailableItemCategoryException exception = assertThrows(
                UnavailableItemCategoryException.class,
                () -> itemService.update(new UpdateItemCommand(
                        "ENG-OIL-5W30-1L",
                        "Engine oil",
                        "INACTIVE_CATEGORY",
                        ItemUnit.EA,
                        50,
                        8500
                ))
        );

        assertEquals("ITM-003", exception.getCode());
    }

    @Test
    void activatesAndDeactivatesItem() {
        itemRepository.save(existingItem("ENG-OIL-5W30-1L", "ENGINE_OIL", false));

        Item activated = itemService.activate("ENG-OIL-5W30-1L");
        assertAll(
                () -> assertTrue(activated.isActive()),
                () -> assertEquals(NOW, activated.getUpdatedAt())
        );

        Item deactivated = itemService.deactivate("ENG-OIL-5W30-1L");

        assertAll(
                () -> assertFalse(deactivated.isActive()),
                () -> assertEquals(NOW, deactivated.getUpdatedAt())
        );
    }

    @Test
    void failsWhenStatusChangeTargetStateIsAlreadyApplied() {
        itemRepository.save(existingItem("ACTIVE-ITEM", "ENGINE_OIL", true));
        itemRepository.save(existingItem("INACTIVE-ITEM", "ENGINE_OIL", false));

        assertAll(
                () -> assertThrows(InvalidItemStatusException.class, () -> itemService.activate("ACTIVE-ITEM")),
                () -> assertThrows(InvalidItemStatusException.class, () -> itemService.deactivate("INACTIVE-ITEM"))
        );
    }

    @Test
    void failsWhenChangingStatusForMissingItem() {
        assertAll(
                () -> assertThrows(ItemNotFoundException.class, () -> itemService.activate("UNKNOWN")),
                () -> assertThrows(ItemNotFoundException.class, () -> itemService.deactivate("UNKNOWN"))
        );
    }

    @Test
    void searchesItems() {
        SearchItemsQuery query = new SearchItemsQuery(
                " oil ",
                " ENGINE_OIL ",
                true,
                0,
                20,
                ItemSortBy.NAME,
                SortDirection.ASC
        );

        PageResult<Item> result = itemService.search(query);

        assertAll(
                () -> assertSame(query, itemRepository.lastQuery),
                () -> assertEquals(0, result.page()),
                () -> assertEquals(20, result.size())
        );
    }

    @Test
    void searchesItemViewsWithRootCategoryExpansion() {
        itemRepository.save(existingItem("ENG-OIL-5W30-1L", "ENGINE_OIL", true));
        itemCategoryRepository.addRootCategory("ENGINE");
        itemCategoryRepository.addSubCategory("ENGINE_OIL", "ENGINE");
        itemCategoryRepository.addSubCategory("ENGINE_FILTER", "ENGINE");

        PageResult<ItemView> result = itemService.searchViews(new SearchItemsQuery(
                "oil",
                "ENGINE",
                true,
                0,
                10,
                ItemSortBy.UPDATED_AT,
                SortDirection.DESC
        ));

        assertAll(
                () -> assertEquals(1, result.totalElements()),
                () -> assertEquals(List.of("ENGINE", "ENGINE_OIL", "ENGINE_FILTER"), itemRepository.lastViewQuery.categoryCodes())
        );
    }

    @Test
    void failsWhenUpdatingInactiveItemWithCategorySelection() {
        itemRepository.save(existingItem("ENG-OIL-5W30-1L", "ENGINE_OIL", false));
        itemCategoryRepository.addRootCategory("ENGINE");
        itemCategoryRepository.addSubCategory("ENGINE_OIL", "ENGINE");

        assertThrows(
                InactiveItemCannotBeModifiedException.class,
                () -> itemService.updateSelection(new UpdateItemSelectionCommand(
                        "ENG-OIL-5W30-1L",
                        "Engine oil",
                        "ENGINE",
                        "ENGINE_OIL",
                        ItemUnit.EA,
                        50,
                        8500
                ))
        );
    }

    @Test
    void returnsSupportedUnits() {
        assertEquals(List.of(ItemUnit.EA, ItemUnit.BOX, ItemUnit.SET, ItemUnit.L), itemService.getUnits());
    }

    private static Item existingItem(String sku, String categoryCode, boolean active) {
        return Item.of(
                sku,
                "Engine oil",
                categoryCode,
                ItemUnit.EA,
                50,
                8500,
                active,
                Instant.parse("2026-06-01T00:00:00Z"),
                Instant.parse("2026-06-01T00:00:00Z")
        );
    }

    private static class FakeItemRepository implements ItemRepository {

        private final Map<String, Item> items = new LinkedHashMap<>();
        private SearchItemsQuery lastQuery;
        private SearchItemViewsQuery lastViewQuery;

        @Override
        public Optional<Item> findBySku(String sku) {
            return Optional.ofNullable(items.get(sku));
        }

        @Override
        public List<Item> findBySkus(List<String> skus) {
            List<Item> found = new ArrayList<>();
            for (String sku : skus) {
                Item item = items.get(sku);
                if (item != null) {
                    found.add(item);
                }
            }
            return found;
        }

        @Override
        public Optional<ItemView> findViewBySku(String sku) {
            return findBySku(sku).map(FakeItemRepository::toView);
        }

        @Override
        public boolean existsBySku(String sku) {
            return items.containsKey(sku);
        }

        @Override
        public PageResult<Item> search(SearchItemsQuery query) {
            lastQuery = query;
            List<Item> content = new ArrayList<>(items.values());
            return new PageResult<>(content, query.page(), query.size(), content.size());
        }

        @Override
        public PageResult<ItemView> searchViews(SearchItemViewsQuery query) {
            lastViewQuery = query;
            List<ItemView> content = items.values().stream()
                    .map(FakeItemRepository::toView)
                    .toList();
            return new PageResult<>(content, query.page(), query.size(), content.size());
        }

        @Override
        public Item save(Item item) {
            items.put(item.getSku(), item);
            return item;
        }

        private static ItemView toView(Item item) {
            return new ItemView(
                    item.getSku(),
                    item.getName(),
                    item.getCategoryCode(),
                    "Category",
                    "ENGINE",
                    "Engine",
                    item.getUnit(),
                    item.getSafetyStock(),
                    item.getUnitPrice(),
                    item.isActive(),
                    item.getCreatedAt(),
                    item.getUpdatedAt()
            );
        }
    }

    private static class FakeItemCategoryRepository implements ItemCategoryRepository {

        private final Map<String, ItemCategory> categories = new LinkedHashMap<>();

        void addActiveCategory(String code) {
            categories.put(code, ItemCategory.subCategory(code, code, "ENGINE", 0, true));
        }

        void addRootCategory(String code) {
            categories.put(code, ItemCategory.root(code, code, 0, true));
        }

        void addSubCategory(String code, String parentCode) {
            categories.put(code, ItemCategory.subCategory(code, code, parentCode, 0, true));
        }

        @Override
        public Optional<ItemCategory> findByCode(String code) {
            return Optional.ofNullable(categories.get(code));
        }

        @Override
        public Optional<ItemCategory> findActiveByCode(String code) {
            return Optional.ofNullable(categories.get(code));
        }

        @Override
        public List<ItemCategory> findRootCategories() {
            return List.of();
        }

        @Override
        public List<ItemCategory> findSubCategories(String parentCode) {
            return categories.values().stream()
                    .filter(category -> parentCode.equals(category.getParentCode()))
                    .toList();
        }

        @Override
        public boolean existsActiveByCode(String code) {
            return categories.containsKey(code);
        }

        @Override
        public boolean existsActiveRootByCode(String code) {
            return Optional.ofNullable(categories.get(code))
                    .filter(category -> category.getDepth() == ItemCategory.ROOT_DEPTH)
                    .isPresent();
        }

        @Override
        public boolean existsActiveSubCategoryOf(String parentCode, String subCategoryCode) {
            return Optional.ofNullable(categories.get(subCategoryCode))
                    .filter(category -> category.getDepth() == ItemCategory.SUB_CATEGORY_DEPTH)
                    .filter(category -> parentCode.equals(category.getParentCode()))
                    .isPresent();
        }

        @Override
        public ItemCategory save(ItemCategory category) {
            categories.put(category.getCode(), category);
            return category;
        }
    }
}
