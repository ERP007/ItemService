package com.fallguys.itemservice.infrastructure.seed;

import com.fallguys.itemservice.domain.ItemCategoryRepository;
import com.fallguys.itemservice.domain.ItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class MasterItemSeedService {

    private final ItemCategoryRepository itemCategoryRepository;
    private final ItemRepository itemRepository;
    private final MasterItemCsvParser csvParser;
    private final Clock clock;

    @Autowired
    public MasterItemSeedService(ItemCategoryRepository itemCategoryRepository, ItemRepository itemRepository) {
        this(itemCategoryRepository, itemRepository, new MasterItemCsvParser(), Clock.systemUTC());
    }

    MasterItemSeedService(
            ItemCategoryRepository itemCategoryRepository,
            ItemRepository itemRepository,
            MasterItemCsvParser csvParser,
            Clock clock
    ) {
        this.itemCategoryRepository = Objects.requireNonNull(itemCategoryRepository, "itemCategoryRepository");
        this.itemRepository = Objects.requireNonNull(itemRepository, "itemRepository");
        this.csvParser = Objects.requireNonNull(csvParser, "csvParser");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    @Transactional
    public MasterItemSeedResult seed(Resource resource) {
        Resource seedResource = requireResource(resource);
        List<MasterItemCsvRow> rows = readRows(seedResource);

        int categoriesCreated = 0;
        int categoriesSkipped = 0;
        for (MasterItemCategoryDefinition category : MasterItemCategoryMapping.allCategories()) {
            if (itemCategoryRepository.findByCode(category.code()).isPresent()) {
                categoriesSkipped++;
                continue;
            }
            itemCategoryRepository.save(category.toDomain());
            categoriesCreated++;
        }

        Instant now = clock.instant();
        int itemsCreated = 0;
        int itemsSkipped = 0;
        for (MasterItemCsvRow row : rows) {
            if (itemRepository.existsBySku(row.sku())) {
                itemsSkipped++;
                continue;
            }
            itemRepository.save(row.toItem(now));
            itemsCreated++;
        }

        return new MasterItemSeedResult(categoriesCreated, categoriesSkipped, itemsCreated, itemsSkipped);
    }

    private List<MasterItemCsvRow> readRows(Resource resource) {
        try (InputStream inputStream = resource.getInputStream()) {
            return csvParser.parse(inputStream, resource.getDescription());
        } catch (IOException ex) {
            throw new MasterItemSeedException("부품 마스터 시드 CSV를 읽을 수 없습니다: " + resource.getDescription(), ex);
        }
    }

    private static Resource requireResource(Resource resource) {
        if (resource == null) {
            throw new MasterItemSeedException("부품 마스터 시드 CSV 리소스는 필수입니다.");
        }
        if (!resource.exists()) {
            throw new MasterItemSeedException("부품 마스터 시드 CSV 리소스가 존재하지 않습니다: " + resource.getDescription());
        }
        return resource;
    }
}
