package com.fallguys.itemservice.domain;

import com.fallguys.itemservice.config.ItemCacheKeys;
import com.fallguys.itemservice.config.ItemCacheNames;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

@SpringJUnitConfig(classes = ItemServiceCacheTest.TestConfig.class)
class ItemServiceCacheTest {

    private static final Instant NOW = Instant.parse("2026-06-07T00:00:00Z");
    private static final Clock CLOCK = Clock.fixed(NOW, ZoneOffset.UTC);
    private static final String SKU = "ENG-OIL-5W30-1L";

    @Autowired
    private ItemService itemService;

    @Autowired
    private FakeItemRepository itemRepository;

    @Autowired
    private FakeItemCategoryRepository itemCategoryRepository;

    @Autowired
    private CacheManager cacheManager;

    @BeforeEach
    void setUp() {
        itemRepository.clear();
        itemCategoryRepository.clear();
        cache().clear();
    }

    @Test
    void getViewBySkuCachesByNormalizedSku() {
        itemRepository.save(existingItem(SKU, "ENGINE_OIL", true));

        itemService.getViewBySku(" " + SKU + " ");
        ItemView cached = itemService.getViewBySku(SKU);

        assertEquals(SKU, cached.sku());
        assertEquals(1, itemRepository.findViewBySkuCalls);
        assertNotNull(cache().get(ItemCacheKeys.detail(SKU), ItemView.class));
    }

    @Test
    void createViewWarmsItemDetailCache() {
        itemCategoryRepository.addActiveCategory("ENGINE_OIL");

        ItemView created = itemService.createView(new CreateItemCommand(
                SKU,
                "Engine oil",
                "ENGINE_OIL",
                ItemUnit.EA,
                50,
                8500
        ));
        itemRepository.findViewBySkuCalls = 0;

        ItemView cached = itemService.getViewBySku(SKU);

        assertEquals(created, cached);
        assertEquals(0, itemRepository.findViewBySkuCalls);
    }

    @Test
    void updateSelectionRefreshesItemDetailCache() {
        itemRepository.save(existingItem(SKU, "ENGINE_OIL", true));
        itemCategoryRepository.addRootCategory("ENGINE");
        itemCategoryRepository.addSubCategory("ENGINE_FILTER", "ENGINE");
        itemService.getViewBySku(SKU);

        ItemView updated = itemService.updateSelection(new UpdateItemSelectionCommand(
                SKU,
                "Oil filter",
                "ENGINE",
                "ENGINE_FILTER",
                ItemUnit.SET,
                10,
                12000
        ));
        int viewLookupCountAfterUpdate = itemRepository.findViewBySkuCalls;

        ItemView cached = itemService.getViewBySku(SKU);

        assertEquals(updated, cached);
        assertEquals("Oil filter", cached.name());
        assertEquals(viewLookupCountAfterUpdate, itemRepository.findViewBySkuCalls);
    }

    @Test
    void updateEvictsItemDetailCache() {
        itemRepository.save(existingItem(SKU, "ENGINE_OIL", true));
        itemCategoryRepository.addActiveCategory("ENGINE_FILTER");
        itemService.getViewBySku(SKU);

        itemService.update(new UpdateItemCommand(
                SKU,
                "Oil filter",
                "ENGINE_FILTER",
                ItemUnit.SET,
                10,
                12000
        ));

        assertNull(cache().get(ItemCacheKeys.detail(SKU)));
    }

    @Test
    void statusChangesEvictItemDetailCache() {
        itemRepository.save(existingItem(SKU, "ENGINE_OIL", false));
        itemService.getViewBySku(SKU);

        itemService.activate(SKU);

        assertNull(cache().get(ItemCacheKeys.detail(SKU)));

        itemService.getViewBySku(SKU);
        itemService.deactivate(SKU);

        assertNull(cache().get(ItemCacheKeys.detail(SKU)));
    }

    private Cache cache() {
        Cache cache = cacheManager.getCache(ItemCacheNames.ITEM_DETAIL);
        assertNotNull(cache);
        return cache;
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

    @Configuration
    @EnableCaching
    static class TestConfig {

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager(ItemCacheNames.ITEM_DETAIL);
        }

        @Bean
        FakeItemRepository itemRepository() {
            return new FakeItemRepository();
        }

        @Bean
        FakeItemCategoryRepository itemCategoryRepository() {
            return new FakeItemCategoryRepository();
        }

        @Bean
        FakeItemSnapshotEventPublisher itemSnapshotEventPublisher() {
            return new FakeItemSnapshotEventPublisher();
        }

        @Bean
        ItemService itemService(
                FakeItemRepository itemRepository,
                FakeItemCategoryRepository itemCategoryRepository,
                FakeItemSnapshotEventPublisher itemSnapshotEventPublisher
        ) {
            return new ItemService(itemRepository, itemCategoryRepository, itemSnapshotEventPublisher, CLOCK);
        }
    }

    static class FakeItemRepository implements ItemRepository {

        private final Map<String, Item> items = new LinkedHashMap<>();
        private int findViewBySkuCalls;

        void clear() {
            items.clear();
            findViewBySkuCalls = 0;
        }

        @Override
        public Optional<Item> findBySku(String sku) {
            return Optional.ofNullable(items.get(sku));
        }

        @Override
        public List<Item> findBySkus(List<String> skus) {
            return skus.stream()
                    .map(items::get)
                    .filter(java.util.Objects::nonNull)
                    .toList();
        }

        @Override
        public Optional<ItemView> findViewBySku(String sku) {
            findViewBySkuCalls++;
            return findBySku(sku).map(FakeItemRepository::toView);
        }

        @Override
        public boolean existsBySku(String sku) {
            return items.containsKey(sku);
        }

        @Override
        public PageResult<Item> search(SearchItemsQuery query) {
            List<Item> content = new ArrayList<>(items.values());
            return new PageResult<>(content, query.page(), query.size(), content.size());
        }

        @Override
        public PageResult<ItemView> searchViews(SearchItemViewsQuery query) {
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

    static class FakeItemCategoryRepository implements ItemCategoryRepository {

        private final Map<String, ItemCategory> categories = new LinkedHashMap<>();

        void clear() {
            categories.clear();
        }

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
            return categories.values().stream()
                    .filter(category -> category.getDepth() == ItemCategory.ROOT_DEPTH)
                    .toList();
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

    static class FakeItemSnapshotEventPublisher implements ItemSnapshotEventPublisher {

        @Override
        public void publishChanged(Item item) {
        }
    }
}
