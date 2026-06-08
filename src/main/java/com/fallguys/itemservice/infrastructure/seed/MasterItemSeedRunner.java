package com.fallguys.itemservice.infrastructure.seed;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class MasterItemSeedRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(MasterItemSeedRunner.class);

    private final MasterItemSeedService seedService;
    private final boolean enabled;
    private final Resource resource;

    public MasterItemSeedRunner(
            MasterItemSeedService seedService,
            @Value("${item.master-seed.enabled:true}") boolean enabled,
            @Value("${item.master-seed.location:classpath:data/items.csv}") Resource resource
    ) {
        this.seedService = seedService;
        this.enabled = enabled;
        this.resource = resource;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!enabled) {
            log.info("Item master seed is disabled.");
            return;
        }

        MasterItemSeedResult result = seedService.seed(resource);
        log.info(
                "Item master seed completed. categoriesCreated={}, categoriesSkipped={}, itemsCreated={}, itemsSkipped={}",
                result.categoriesCreated(),
                result.categoriesSkipped(),
                result.itemsCreated(),
                result.itemsSkipped()
        );
    }
}
