package com.fallguys.itemservice.domain;

import java.util.Optional;
import java.util.List;

public interface ItemRepository {

    Optional<Item> findBySku(String sku);

    List<Item> findBySkus(List<String> skus);

    Optional<ItemView> findViewBySku(String sku);

    boolean existsBySku(String sku);

    PageResult<Item> search(SearchItemsQuery query);

    PageResult<ItemView> searchViews(SearchItemViewsQuery query);

    Item save(Item item);
}
