package com.fallguys.itemservice.domain;

import java.util.Optional;

public interface ItemRepository {

    Optional<Item> findBySku(String sku);

    boolean existsBySku(String sku);

    PageResult<Item> search(SearchItemsQuery query);

    Item save(Item item);
}
