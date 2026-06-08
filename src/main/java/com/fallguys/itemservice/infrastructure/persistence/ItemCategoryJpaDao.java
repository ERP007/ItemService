package com.fallguys.itemservice.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ItemCategoryJpaDao extends JpaRepository<ItemCategoryEntity, String> {

    Optional<ItemCategoryEntity> findByCodeAndActiveTrue(String code);

    List<ItemCategoryEntity> findByDepthAndActiveTrueOrderByDisplayOrderAscNameAsc(int depth);

    List<ItemCategoryEntity> findByParentCodeAndActiveTrueOrderByDisplayOrderAscNameAsc(String parentCode);

    boolean existsByCodeAndActiveTrue(String code);

    boolean existsByCodeAndDepthAndActiveTrue(String code, int depth);

    boolean existsByCodeAndParentCodeAndDepthAndActiveTrue(String code, String parentCode, int depth);
}
