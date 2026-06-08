package com.fallguys.itemservice.domain;

import java.util.Optional;

public interface ItemRepository {

    Optional<Item> findBySku(String sku);

    Optional<ItemView> findViewBySku(String sku);

    boolean existsBySku(String sku);

    PageResult<Item> search(SearchItemsQuery query);

    PageResult<ItemView> searchViews(SearchItemViewsQuery query);

    Item save(Item item);
}
