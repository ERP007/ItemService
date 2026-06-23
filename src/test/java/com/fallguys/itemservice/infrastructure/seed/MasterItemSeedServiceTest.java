package com.fallguys.itemservice.infrastructure.seed;

import com.fallguys.itemservice.infrastructure.persistence.ItemCategoryRepositoryAdapter;
import com.fallguys.itemservice.infrastructure.persistence.ItemRepositoryAdapter;
import jakarta.persistence.EntityManager;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationInitializer;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@Import({
        MasterItemSeedServiceTest.FlywayTestConfiguration.class,
        ItemRepositoryAdapter.class,
        ItemCategoryRepositoryAdapter.class,
        MasterItemSeedService.class
})
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:item_seed_test;MODE=PostgreSQL;INIT=CREATE DOMAIN IF NOT EXISTS JSONB AS JSON;DB_CLOSE_DELAY=-1",
        "spring.datasource.driver-class-name=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=validate",
        "messaging.outbox.relay.enabled=false",
        "spring.flyway.enabled=true"
})
class MasterItemSeedServiceTest {

    @Autowired
    private MasterItemSeedService seedService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private EntityManager entityManager;

    @Test
    void seedsCategoriesAndItemsFromClasspathCsv() {
        MasterItemSeedResult result = seedService.seed(new ClassPathResource("data/items.csv"));
        entityManager.flush();

        assertAll(
                () -> assertEquals(9, result.categoriesCreated()),
                () -> assertEquals(0, result.categoriesSkipped()),
                () -> assertEquals(42, result.itemsCreated()),
                () -> assertEquals(0, result.itemsSkipped()),
                () -> assertEquals(9, count("item_categories")),
                () -> assertEquals(42, count("items")),
                () -> assertEquals("ENGINE_LUBRICATION", itemCategoryCode("ENG-OIL-5W30-1L")),
                () -> assertEquals("BRAKE", itemCategoryCode("BRK-PAD-FR-001")),
                () -> assertEquals(false, itemActive("ENG-OIL-15W40-4L"))
        );
    }

    @Test
    void skipsExistingItemsAndDoesNotOverwriteThem() {
        seedService.seed(new ClassPathResource("data/items.csv"));
        entityManager.flush();
        entityManager.clear();

        jdbcTemplate.update("""
                update items
                set name = ?, unit_price = ?
                where sku = ?
                """, "수동 수정된 엔진오일", 1, "ENG-OIL-5W30-1L");

        MasterItemSeedResult second = seedService.seed(new ClassPathResource("data/items.csv"));
        entityManager.flush();

        assertAll(
                () -> assertEquals(0, second.categoriesCreated()),
                () -> assertEquals(9, second.categoriesSkipped()),
                () -> assertEquals(0, second.itemsCreated()),
                () -> assertEquals(42, second.itemsSkipped()),
                () -> assertEquals(42, count("items")),
                () -> assertEquals("수동 수정된 엔진오일", itemName("ENG-OIL-5W30-1L")),
                () -> assertEquals(1, itemUnitPrice("ENG-OIL-5W30-1L"))
        );
    }

    private long count(String tableName) {
        return jdbcTemplate.queryForObject("select count(*) from " + tableName, Long.class);
    }

    private String itemCategoryCode(String sku) {
        return jdbcTemplate.queryForObject("select category_code from items where sku = ?", String.class, sku);
    }

    private boolean itemActive(String sku) {
        return jdbcTemplate.queryForObject("select active from items where sku = ?", Boolean.class, sku);
    }

    private String itemName(String sku) {
        return jdbcTemplate.queryForObject("select name from items where sku = ?", String.class, sku);
    }

    private int itemUnitPrice(String sku) {
        return jdbcTemplate.queryForObject("select unit_price from items where sku = ?", Integer.class, sku);
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
