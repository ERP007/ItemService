package com.fallguys.itemservice.infrastructure.persistence;

import com.fallguys.itemservice.domain.ItemCategory;
import com.fallguys.itemservice.domain.ItemCategoryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class ItemCategoryRepositoryAdapter implements ItemCategoryRepository {

    private final ItemCategoryJpaDao jpaDao;

    public ItemCategoryRepositoryAdapter(ItemCategoryJpaDao jpaDao) {
        this.jpaDao = jpaDao;
    }

    @Override
    public Optional<ItemCategory> findByCode(String code) {
        String normalizedCode = normalizeCode(code);
        if (normalizedCode == null) {
            return Optional.empty();
        }
        return jpaDao.findById(normalizedCode).map(ItemCategoryEntity::toDomain);
    }

    @Override
    public Optional<ItemCategory> findActiveByCode(String code) {
        String normalizedCode = normalizeCode(code);
        if (normalizedCode == null) {
            return Optional.empty();
        }
        return jpaDao.findByCodeAndActiveTrue(normalizedCode).map(ItemCategoryEntity::toDomain);
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

    @Override
    public boolean existsActiveRootByCode(String code) {
        String normalizedCode = normalizeCode(code);
        return normalizedCode != null
                && jpaDao.existsByCodeAndDepthAndActiveTrue(normalizedCode, ItemCategory.ROOT_DEPTH);
    }

    @Override
    public boolean existsActiveSubCategoryOf(String parentCode, String subCategoryCode) {
        String normalizedParentCode = normalizeCode(parentCode);
        String normalizedSubCategoryCode = normalizeCode(subCategoryCode);
        return normalizedParentCode != null
                && normalizedSubCategoryCode != null
                && jpaDao.existsByCodeAndParentCodeAndDepthAndActiveTrue(
                normalizedSubCategoryCode,
                normalizedParentCode,
                ItemCategory.SUB_CATEGORY_DEPTH
        );
    }

    @Override
    public ItemCategory save(ItemCategory category) {
        ItemCategoryEntity entity = jpaDao.findById(category.getCode())
                .map(existing -> existing.update(category))
                .orElseGet(() -> ItemCategoryEntity.from(category));
        return jpaDao.save(entity).toDomain();
    }

    private static String normalizeCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        return code.trim();
    }
}
