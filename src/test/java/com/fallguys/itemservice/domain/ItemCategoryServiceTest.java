package com.fallguys.itemservice.domain;

import com.fallguys.itemservice.domain.exception.CategoryNotFoundException;
import com.fallguys.itemservice.domain.exception.InvalidItemException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ItemCategoryServiceTest {

    private final FakeItemCategoryRepository itemCategoryRepository = new FakeItemCategoryRepository();
    private final ItemCategoryService itemCategoryService = new ItemCategoryService(itemCategoryRepository);

    @Test
    void findsRootCategories() {
        ItemCategory engine = ItemCategory.root("ENGINE", "Engine", 1, true);
        itemCategoryRepository.save(engine);
        itemCategoryRepository.save(ItemCategory.subCategory("ENGINE_OIL", "Engine oil", "ENGINE", 1, true));

        List<ItemCategory> found = itemCategoryService.findRootCategories();

        assertEquals(List.of(engine), found);
    }

    @Test
    void findsSubCategoriesForActiveRootCategory() {
        ItemCategory oil = ItemCategory.subCategory("ENGINE_OIL", "Engine oil", "ENGINE", 1, true);
        itemCategoryRepository.save(ItemCategory.root("ENGINE", "Engine", 1, true));
        itemCategoryRepository.save(oil);
        itemCategoryRepository.save(ItemCategory.subCategory("ENGINE_FILTER", "Engine filter", "ENGINE", 2, false));

        List<ItemCategory> found = itemCategoryService.findSubCategories(" ENGINE ");

        assertAll(
                () -> assertEquals(1, found.size()),
                () -> assertSame(oil, found.get(0))
        );
    }

    @Test
    void failsWhenRootCategoryCodeIsMissing() {
        assertAll(
                () -> assertThrows(InvalidItemException.class, () -> itemCategoryService.findSubCategories(null)),
                () -> assertThrows(InvalidItemException.class, () -> itemCategoryService.findSubCategories(" "))
        );
    }

    @Test
    void failsWhenRootCategoryDoesNotExist() {
        CategoryNotFoundException exception = assertThrows(
                CategoryNotFoundException.class,
                () -> itemCategoryService.findSubCategories("UNKNOWN")
        );

        assertEquals("ITM-003", exception.getCode());
    }

    private static class FakeItemCategoryRepository implements ItemCategoryRepository {

        private final Map<String, ItemCategory> categories = new LinkedHashMap<>();

        @Override
        public Optional<ItemCategory> findByCode(String code) {
            return Optional.ofNullable(categories.get(code));
        }

        @Override
        public Optional<ItemCategory> findActiveByCode(String code) {
            return findByCode(code).filter(ItemCategory::isActive);
        }

        @Override
        public List<ItemCategory> findRootCategories() {
            return categories.values().stream()
                    .filter(ItemCategory::isActive)
                    .filter(category -> category.getDepth() == ItemCategory.ROOT_DEPTH)
                    .toList();
        }

        @Override
        public List<ItemCategory> findSubCategories(String parentCode) {
            List<ItemCategory> found = new ArrayList<>();
            for (ItemCategory category : categories.values()) {
                if (category.isActive()
                        && category.getDepth() == ItemCategory.SUB_CATEGORY_DEPTH
                        && parentCode.equals(category.getParentCode())) {
                    found.add(category);
                }
            }
            return found;
        }

        @Override
        public boolean existsActiveByCode(String code) {
            return findActiveByCode(code).isPresent();
        }

        @Override
        public boolean existsActiveRootByCode(String code) {
            return findActiveByCode(code)
                    .filter(category -> category.getDepth() == ItemCategory.ROOT_DEPTH)
                    .isPresent();
        }

        @Override
        public boolean existsActiveSubCategoryOf(String parentCode, String subCategoryCode) {
            return findActiveByCode(subCategoryCode)
                    .filter(category -> category.getDepth() == ItemCategory.SUB_CATEGORY_DEPTH)
                    .filter(category -> parentCode.equals(category.getParentCode()))
                    .isPresent();
        }

        @Override
        public ItemCategory save(ItemCategory category) {
            categories.put(category.getCode(), category);
            return category;
        }
    }
}
