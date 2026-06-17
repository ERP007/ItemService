package com.fallguys.itemservice.domain;

public interface InventoryItemSynchronizer {

    void syncName(String sku, String itemName);

    void syncUnit(String sku, ItemUnit itemUnit);

    void syncActive(String sku, boolean active);
}
