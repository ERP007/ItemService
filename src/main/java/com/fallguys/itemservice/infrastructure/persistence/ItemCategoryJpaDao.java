package com.fallguys.itemservice.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ItemCategoryJpaDao extends JpaRepository<ItemCategoryEntity, String> {

    List<ItemCategoryEntity> findByDepthAndActiveTrueOrderByDisplayOrderAscNameAsc(int depth);

    List<ItemCategoryEntity> findByParentCodeAndActiveTrueOrderByDisplayOrderAscNameAsc(String parentCode);

    boolean existsByCodeAndActiveTrue(String code);
}
