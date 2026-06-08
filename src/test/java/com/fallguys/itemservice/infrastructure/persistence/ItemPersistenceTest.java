package com.fallguys.itemservice.infrastructure.persistence;

import com.fallguys.itemservice.domain.Item;
import com.fallguys.itemservice.domain.ItemCategory;
import com.fallguys.itemservice.domain.ItemCategoryRepository;
import com.fallguys.itemservice.domain.ItemRepository;
import com.fallguys.itemservice.domain.ItemSortBy;
import com.fallguys.itemservice.domain.ItemUnit;
import com.fallguys.itemservice.domain.PageResult;
import com.fallguys.itemservice.domain.SearchItemViewsQuery;
import com.fallguys.itemservice.domain.SearchItemsQuery;
import com.fallguys.itemservice.domain.SortDirection;
import com.fallguys.itemservice.domain.ItemView;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@Import({
        ItemPersistenceTest.FlywayTestConfiguration.class,
        ItemRepositoryAdapter.class,
        ItemCategoryRepositoryAdapter.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:item_persistence_test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "spring.flyway.enabled=true"
})
class ItemPersistenceTest {

    private static final Instant CREATED_AT = Instant.parse("2026-06-07T00:00:00Z");
    private static final Instant UPDATED_AT = Instant.parse("2026-06-07T01:00:00Z");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ItemRepository itemRepository;

    @Autowired
    private ItemCategoryRepository itemCategoryRepository;

    @Autowired
    private ItemCategoryJpaDao itemCategoryJpaDao;

    @Test
    void flywayCreatesItemMasterTables() {
        assertAll(
                () -> assertDoesNotThrow(() -> jdbcTemplate.queryForObject("select count(*) from item_categories", Long.class)),
                () -> assertDoesNotThrow(() -> jdbcTemplate.queryForObject("select count(*) from items", Long.class))
        );
    }

    @Test
    void mapsItemEntityAndUpdatesExistingEntity() {
        Item item = item("ENG-OIL-5W30-1L", "Engine oil", "ENGINE_OIL", ItemUnit.EA, 50, 8500, true);
        ItemEntity entity = ItemEntity.from(item);

        Item mapped = entity.toDomain();
        Item updated = Item.of(
                "ENG-OIL-5W30-1L",
                "Updated engine oil",
                "ENGINE_OIL",
                ItemUnit.SET,
                20,
                9000,
                false,
                CREATED_AT,
                UPDATED_AT
        );
        entity.update(updated);
        Item updatedMapped = entity.toDomain();

        assertAll(
                () -> assertEquals(item.getSku(), mapped.getSku()),
                () -> assertEquals(item.getName(), mapped.getName()),
                () -> assertEquals(ItemUnit.EA, mapped.getUnit()),
                () -> assertEquals("Updated engine oil", updatedMapped.getName()),
                () -> assertEquals(ItemUnit.SET, updatedMapped.getUnit()),
                () -> assertFalse(updatedMapped.isActive()),
                () -> assertEquals(UPDATED_AT, updatedMapped.getUpdatedAt())
        );
    }

    @Test
    void savesFindsAndUpdatesItems() {
        saveCategory(ItemCategory.root("ENGINE", "Engine", 1, true));
        saveCategory(ItemCategory.subCategory("ENGINE_OIL", "Engine oil", "ENGINE", 1, true));

        Item saved = itemRepository.save(item("ENG-OIL-5W30-1L", "Engine oil", "ENGINE_OIL", ItemUnit.EA, 50, 8500, true));
        Optional<Item> found = itemRepository.findBySku("ENG-OIL-5W30-1L");
        Item updated = Item.of(
                saved.getSku(),
                "Engine oil 5W-30",
                saved.getCategoryCode(),
                saved.getUnit(),
                40,
                9000,
                false,
                saved.getCreatedAt(),
                UPDATED_AT
        );
        itemRepository.save(updated);
        Item refound = itemRepository.findBySku("ENG-OIL-5W30-1L").orElseThrow();

        assertAll(
                () -> assertTrue(itemRepository.existsBySku("ENG-OIL-5W30-1L")),
                () -> assertTrue(found.isPresent()),
                () -> assertEquals("Engine oil 5W-30", refound.getName()),
                () -> assertEquals(40, refound.getSafetyStock()),
                () -> assertEquals(9000, refound.getUnitPrice()),
                () -> assertFalse(refound.isActive())
        );
    }

    @Test
    void searchesItemsWithFiltersSortAndPagination() {
        saveCategory(ItemCategory.root("ENGINE", "Engine", 1, true));
        saveCategory(ItemCategory.subCategory("ENGINE_OIL", "Engine oil", "ENGINE", 1, true));
        saveCategory(ItemCategory.subCategory("ENGINE_FILTER", "Engine filter", "ENGINE", 2, true));
        itemRepository.save(item("ENG-OIL-5W30-1L", "Alpha oil", "ENGINE_OIL", ItemUnit.EA, 50, 1000, true));
        itemRepository.save(item("ENG-OIL-0W20-4L", "Bravo oil", "ENGINE_OIL", ItemUnit.EA, 30, 3000, true));
        itemRepository.save(item("ENG-OIL-15W40-4L", "Charlie oil", "ENGINE_OIL", ItemUnit.EA, 10, 2000, false));
        itemRepository.save(item("ENG-FILTER-A", "Oil filter", "ENGINE_FILTER", ItemUnit.SET, 5, 5000, true));

        PageResult<Item> firstPage = itemRepository.search(new SearchItemsQuery(
                "oil",
                "ENGINE_OIL",
                true,
                0,
                1,
                ItemSortBy.UNIT_PRICE,
                SortDirection.DESC
        ));

        assertAll(
                () -> assertEquals(2, firstPage.totalElements()),
                () -> assertEquals(2, firstPage.totalPages()),
                () -> assertTrue(firstPage.hasNext()),
                () -> assertEquals(1, firstPage.content().size()),
                () -> assertEquals("ENG-OIL-0W20-4L", firstPage.content().getFirst().getSku())
        );
    }

    @Test
    void searchesItemViewsWithCategoryNames() {
        saveCategory(ItemCategory.root("ENGINE", "엔진", 1, true));
        saveCategory(ItemCategory.subCategory("ENGINE_OIL", "윤활계통", "ENGINE", 1, true));
        saveCategory(ItemCategory.subCategory("ENGINE_FILTER", "필터", "ENGINE", 2, true));
        itemRepository.save(item("ENG-OIL-5W30-1L", "Alpha oil", "ENGINE_OIL", ItemUnit.EA, 50, 1000, true));
        itemRepository.save(item("ENG-FILTER-A", "Oil filter", "ENGINE_FILTER", ItemUnit.SET, 5, 5000, true));

        PageResult<ItemView> firstPage = itemRepository.searchViews(new SearchItemViewsQuery(
                "oil",
                List.of("ENGINE_OIL", "ENGINE_FILTER"),
                true,
                0,
                1,
                ItemSortBy.SAFETY_STOCK,
                SortDirection.ASC
        ));
        ItemView first = firstPage.content().getFirst();
        Optional<ItemView> found = itemRepository.findViewBySku("ENG-OIL-5W30-1L");

        assertAll(
                () -> assertEquals(2, firstPage.totalElements()),
                () -> assertEquals("ENG-FILTER-A", first.sku()),
                () -> assertEquals("필터", first.categoryName()),
                () -> assertEquals("ENGINE", first.parentCategoryCode()),
                () -> assertEquals("엔진", first.parentCategoryName()),
                () -> assertTrue(found.isPresent()),
                () -> assertEquals("윤활계통", found.orElseThrow().categoryName())
        );
    }

    @Test
    void findsActiveCategories() {
        saveCategory(ItemCategory.root("ENGINE", "Engine", 2, true));
        saveCategory(ItemCategory.root("BRAKE", "Brake", 1, false));
        saveCategory(ItemCategory.subCategory("ENGINE_OIL", "Engine oil", "ENGINE", 2, true));
        saveCategory(ItemCategory.subCategory("ENGINE_FILTER", "Engine filter", "ENGINE", 1, false));

        List<ItemCategory> roots = itemCategoryRepository.findRootCategories();
        List<ItemCategory> subCategories = itemCategoryRepository.findSubCategories(" ENGINE ");

        assertAll(
                () -> assertEquals(1, roots.size()),
                () -> assertEquals("ENGINE", roots.getFirst().getCode()),
                () -> assertEquals(1, subCategories.size()),
                () -> assertEquals("ENGINE_OIL", subCategories.getFirst().getCode()),
                () -> assertTrue(itemCategoryRepository.existsActiveByCode("ENGINE_OIL")),
                () -> assertFalse(itemCategoryRepository.existsActiveByCode("ENGINE_FILTER")),
                () -> assertFalse(itemCategoryRepository.existsActiveByCode("UNKNOWN"))
        );
    }

    private void saveCategory(ItemCategory category) {
        itemCategoryJpaDao.saveAndFlush(ItemCategoryEntity.from(category));
    }

    private static Item item(
            String sku,
            String name,
            String categoryCode,
            ItemUnit unit,
            int safetyStock,
            int unitPrice,
            boolean active
    ) {
        return Item.of(sku, name, categoryCode, unit, safetyStock, unitPrice, active, CREATED_AT, CREATED_AT);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class FlywayTestConfiguration {

        @Bean
        Flyway flyway(DataSource dataSource) {
            return Flyway.configure()
                    .dataSource(dataSource)
                    .locations("classpath:db/migration")
                    .load();
        }

        @Bean
        FlywayMigrationInitializer flywayInitializer(Flyway flyway) {
            return new FlywayMigrationInitializer(flyway);
        }
    }
}
