package com.fallguys.itemservice.domain;

import java.util.List;

public interface ItemCategoryRepository {

    List<ItemCategory> findRootCategories();

    List<ItemCategory> findSubCategories(String parentCode);

    boolean existsActiveByCode(String code);
}
