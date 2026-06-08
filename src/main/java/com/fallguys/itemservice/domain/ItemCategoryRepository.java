package com.fallguys.itemservice.domain;

import java.util.List;
import java.util.Optional;

public interface ItemCategoryRepository {

    Optional<ItemCategory> findByCode(String code);

    Optional<ItemCategory> findActiveByCode(String code);

    List<ItemCategory> findRootCategories();

    List<ItemCategory> findSubCategories(String parentCode);

    boolean existsActiveByCode(String code);

    boolean existsActiveRootByCode(String code);

    boolean existsActiveSubCategoryOf(String parentCode, String subCategoryCode);

    ItemCategory save(ItemCategory category);
}
