package com.fallguys.itemservice.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ItemJpaDao extends JpaRepository<ItemEntity, String>, JpaSpecificationExecutor<ItemEntity> {
}
