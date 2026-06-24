package com.fallguys.itemservice.domain;

public interface ItemSnapshotEventPublisher {

    void publishChanged(Item item);
}
