package com.fallguys.itemservice.infrastructure.persistence;

import com.fallguys.itemservice.domain.ItemCategory;
import com.fallguys.itemservice.domain.ItemCategoryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class ItemCategoryRepositoryAdapter implements ItemCategoryRepository {

    private final ItemCategoryJpaDao jpaDao;

    public ItemCategoryRepositoryAdapter(ItemCategoryJpaDao jpaDao) {
        this.jpaDao = jpaDao;
    }

    @Override
    public List<ItemCategory> findRootCategories() {
        return jpaDao.findByDepthAndActiveTrueOrderByDisplayOrderAscNameAsc(ItemCategory.ROOT_DEPTH).stream()
                .map(ItemCategoryEntity::toDomain)
                .toList();
    }

    @Override
    public List<ItemCategory> findSubCategories(String parentCode) {
        String normalizedParentCode = normalizeCode(parentCode);
        if (normalizedParentCode == null) {
            return List.of();
        }

        return jpaDao.findByParentCodeAndActiveTrueOrderByDisplayOrderAscNameAsc(normalizedParentCode).stream()
                .map(ItemCategoryEntity::toDomain)
                .toList();
    }

    @Override
    public boolean existsActiveByCode(String code) {
        String normalizedCode = normalizeCode(code);
        return normalizedCode != null && jpaDao.existsByCodeAndActiveTrue(normalizedCode);
    }

    private static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return code.trim();
    }
}
