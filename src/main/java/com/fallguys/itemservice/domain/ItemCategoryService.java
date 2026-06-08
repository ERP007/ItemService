package com.fallguys.itemservice.domain;

import com.fallguys.itemservice.domain.exception.CategoryNotFoundException;
import com.fallguys.itemservice.domain.exception.InvalidItemException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;

@Service
public class ItemCategoryService {

    private final ItemCategoryRepository itemCategoryRepository;

    public ItemCategoryService(ItemCategoryRepository itemCategoryRepository) {
        this.itemCategoryRepository = Objects.requireNonNull(itemCategoryRepository, "itemCategoryRepository");
    }

    /**
     * 활성 대분류 목록을 조회한다.
     *
     * 흐름:
     * 1) ItemCategoryRepository로 depth=1, active=true 카테고리를 조회한다.
     *
     * 트랜잭션: 읽기. 저장이나 상태 변경은 수행하지 않는다.
     *
     * 예외: 없음.
     */
    @Transactional(readOnly = true)
    public List<ItemCategory> findRootCategories() {
        return itemCategoryRepository.findRootCategories();
    }

    /**
     * 활성 대분류의 활성 중분류 목록을 조회한다.
     *
     * 흐름:
     * 1) 대분류 코드가 활성 대분류인지 확인한다.
     * 2) 해당 대분류의 활성 중분류를 조회한다.
     *
     * 트랜잭션: 읽기. 저장이나 상태 변경은 수행하지 않는다.
     *
     * 예외:
     * - 대분류 코드 누락: InvalidItemException (롤백 대상 변경 없음)
     * - 대분류 없음: CategoryNotFoundException (롤백 대상 변경 없음)
     */
    @Transactional(readOnly = true)
    public List<ItemCategory> findSubCategories(String categoryCode) {
        String normalizedCategoryCode = requireText(categoryCode, "categoryCode");
        if (!itemCategoryRepository.existsActiveRootByCode(normalizedCategoryCode)) {
            throw new CategoryNotFoundException(normalizedCategoryCode);
        }
        return itemCategoryRepository.findSubCategories(normalizedCategoryCode);
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidItemException(fieldName + " is required.");
        }
        return value.trim();
    }
}
