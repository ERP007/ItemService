package com.fallguys.itemservice.domain;

import com.fallguys.itemservice.domain.exception.DuplicateItemSkuException;
import com.fallguys.itemservice.domain.exception.InvalidItemException;
import com.fallguys.itemservice.domain.exception.InactiveItemCannotBeModifiedException;
import com.fallguys.itemservice.domain.exception.ItemNotFoundException;
import com.fallguys.itemservice.domain.exception.UnavailableItemCategoryException;
import com.fallguys.itemservice.domain.exception.CategoryNotFoundException;
import com.fallguys.itemservice.domain.exception.InvalidItemRequestException;
import com.fallguys.itemservice.domain.exception.ItemErrorCode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

@Service
public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemCategoryRepository itemCategoryRepository;
    private final Clock clock;

    @Autowired
    public ItemService(ItemRepository itemRepository, ItemCategoryRepository itemCategoryRepository) {
        this(itemRepository, itemCategoryRepository, Clock.systemUTC());
    }

    ItemService(ItemRepository itemRepository, ItemCategoryRepository itemCategoryRepository, Clock clock) {
        this.itemRepository = Objects.requireNonNull(itemRepository, "itemRepository");
        this.itemCategoryRepository = Objects.requireNonNull(itemCategoryRepository, "itemCategoryRepository");
        this.clock = Objects.requireNonNull(clock, "clock");
    }

    /**
     * 품목 목록을 검색한다.
     *
     * 흐름:
     * 1) 검색 조건은 SearchItemsQuery 생성 시 정규화·검증된다.
     * 2) ItemRepository에 검색 조건을 위임한다.
     *
     * 트랜잭션: 읽기. 저장이나 상태 변경은 수행하지 않는다.
     *
     * 예외:
     * - 잘못된 검색 조건: InvalidItemException (롤백 대상 변경 없음)
     */
    @Transactional(readOnly = true)
    public PageResult<Item> search(SearchItemsQuery query) {
        SearchItemsQuery validatedQuery = Objects.requireNonNull(query, "query");

        return itemRepository.search(validatedQuery);
    }

    /**
     * API 응답용 품목 목록을 검색한다.
     *
     * 흐름:
     * 1) 선택된 categoryCode가 있으면 활성 카테고리인지 확인한다.
     * 2) 대분류 선택 시 대분류 코드와 하위 중분류 코드를 모두 검색 조건에 포함한다.
     * 3) ItemRepository로 카테고리 표시명을 포함한 목록 조회를 위임한다.
     *
     * 트랜잭션: 읽기. 저장이나 상태 변경은 수행하지 않는다.
     *
     * 예외:
     * - 존재하지 않는 카테고리: CategoryNotFoundException (롤백 대상 변경 없음)
     */
    @Transactional(readOnly = true)
    public PageResult<ItemView> searchViews(SearchItemsQuery query) {
        SearchItemsQuery validatedQuery = Objects.requireNonNull(query, "query");
        List<String> categoryCodes = resolveCategoryCodes(validatedQuery.categoryCode());

        return itemRepository.searchViews(new SearchItemViewsQuery(
                validatedQuery.search(),
                categoryCodes,
                validatedQuery.active(),
                validatedQuery.page(),
                validatedQuery.size(),
                validatedQuery.sortBy(),
                validatedQuery.sortDirection()
        ));
    }

    /**
     * SKU로 품목 단건을 조회한다.
     *
     * 흐름:
     * 1) SKU를 정규화한다.
     * 2) ItemRepository로 단건 조회한다.
     *
     * 트랜잭션: 읽기. 저장이나 상태 변경은 수행하지 않는다.
     *
     * 예외:
     * - SKU 형식 오류: InvalidItemException (롤백 대상 변경 없음)
     * - 품목 없음: ItemNotFoundException (롤백 대상 변경 없음)
     */
    @Transactional(readOnly = true)
    public Item getBySku(String sku) {
        String normalizedSku = requireText(sku, "sku");

        return itemRepository.findBySku(normalizedSku)
                .orElseThrow(() -> new ItemNotFoundException(normalizedSku));
    }

    /**
     * 여러 SKU로 품목을 조회한다.
     *
     * 흐름:
     * 1) 컨트롤러에서 정규화·검증된 SKU 목록을 받는다.
     * 2) ItemRepository로 배치 조회를 위임한다.
     *
     * 트랜잭션: 읽기. 저장이나 상태 변경은 수행하지 않는다.
     *
     * 예외:
     * - null SKU 목록: NullPointerException (호출 계약 위반, 롤백 대상 변경 없음)
     */
    @Transactional(readOnly = true)
    public List<Item> getBySkus(List<String> skus) {
        List<String> validatedSkus = Objects.requireNonNull(skus, "skus");
        if (validatedSkus.isEmpty()) {
            return List.of();
        }

        return itemRepository.findBySkus(validatedSkus);
    }

    /**
     * SKU로 API 응답용 품목 단건을 조회한다.
     *
     * 흐름:
     * 1) SKU를 정규화한다.
     * 2) ItemRepository로 카테고리 표시명을 포함한 단건 조회를 위임한다.
     *
     * 트랜잭션: 읽기. 저장이나 상태 변경은 수행하지 않는다.
     *
     * 예외:
     * - 품목 없음: ItemNotFoundException (롤백 대상 변경 없음)
     */
    @Transactional(readOnly = true)
    public ItemView getViewBySku(String sku) {
        String normalizedSku = requireText(sku, "sku");

        return findViewBySku(normalizedSku);
    }

    /**
     * SKU 사용 가능 여부를 확인한다.
     *
     * 흐름:
     * 1) SKU를 정규화한다.
     * 2) 기존 품목 존재 여부를 조회한다.
     *
     * 트랜잭션: 읽기. 저장이나 상태 변경은 수행하지 않는다.
     *
     * 예외:
     * - SKU 형식 오류: InvalidItemException (롤백 대상 변경 없음)
     */
    @Transactional(readOnly = true)
    public boolean isSkuAvailable(String sku) {
        String normalizedSku = requireText(sku, "sku");

        return !itemRepository.existsBySku(normalizedSku);
    }

    /**
     * 품목을 신규 등록한다.
     *
     * 흐름:
     * 1) SKU 중복 여부를 확인한다.
     * 2) 카테고리 코드가 활성 카테고리인지 확인한다.
     * 3) Item 도메인 모델을 생성하고 저장한다.
     *
     * 트랜잭션: 쓰기. 중복 SKU 또는 카테고리 검증 실패 시 저장하지 않는다.
     *
     * 예외:
     * - SKU 중복: DuplicateItemSkuException (저장 전 중단)
     * - 카테고리 사용 불가: UnavailableItemCategoryException (저장 전 중단)
     * - 품목 불변식 위반: InvalidItemException (저장 전 중단)
     */
    @Transactional
    public Item create(CreateItemCommand command) {
        CreateItemCommand validatedCommand = Objects.requireNonNull(command, "command");

        if (itemRepository.existsBySku(validatedCommand.sku())) {
            throw new DuplicateItemSkuException(validatedCommand.sku());
        }
        validateActiveCategory(validatedCommand.categoryCode());

        Instant now = clock.instant();
        Item item = Item.create(
                validatedCommand.sku(),
                validatedCommand.name(),
                validatedCommand.categoryCode(),
                validatedCommand.unit(),
                validatedCommand.safetyStock(),
                validatedCommand.unitPrice(),
                now
        );
        return itemRepository.save(item);
    }

    /**
     * 품목을 신규 등록하고 API 응답용 상세 데이터를 조회한다.
     *
     * 흐름:
     * 1) create(command)로 도메인 저장 흐름을 수행한다.
     * 2) 저장된 SKU로 카테고리 표시명을 포함한 응답 데이터를 조회한다.
     *
     * 트랜잭션: 쓰기. 등록 검증 실패 시 저장하지 않는다.
     *
     * 예외:
     * - create(command)의 예외와 동일하다.
     */
    @Transactional
    public ItemView createView(CreateItemCommand command) {
        Item item = create(command);

        return findViewBySku(item.getSku());
    }

    /**
     * 기존 품목 정보를 수정한다.
     *
     * 흐름:
     * 1) SKU로 기존 품목을 조회한다.
     * 2) 변경할 카테고리 코드가 활성 카테고리인지 확인한다.
     * 3) Item 도메인 모델을 수정하고 저장한다.
     *
     * 트랜잭션: 쓰기. 품목 없음 또는 카테고리 검증 실패 시 저장하지 않는다.
     *
     * 예외:
     * - 품목 없음: ItemNotFoundException (저장 전 중단)
     * - 카테고리 사용 불가: UnavailableItemCategoryException (저장 전 중단)
     * - 품목 불변식 위반: InvalidItemException (저장 전 중단)
     */
    @Transactional
    public Item update(UpdateItemCommand command) {
        UpdateItemCommand validatedCommand = Objects.requireNonNull(command, "command");
        Item item = getBySku(validatedCommand.sku());
        validateActiveCategory(validatedCommand.categoryCode());

        item.update(
                validatedCommand.name(),
                validatedCommand.categoryCode(),
                validatedCommand.unit(),
                validatedCommand.safetyStock(),
                validatedCommand.unitPrice(),
                clock.instant()
        );
        return itemRepository.save(item);
    }

    /**
     * 대분류·중분류 선택값을 검증한 뒤 품목 기본 정보를 수정한다.
     *
     * 흐름:
     * 1) SKU로 기존 품목을 조회한다.
     * 2) 비활성 품목이면 수정을 중단한다.
     * 3) 대분류와 중분류가 모두 활성이고 부모-자식 관계가 맞는지 확인한다.
     * 4) 최종 중분류 코드로 품목을 수정하고 저장한다.
     *
     * 트랜잭션: 쓰기. 검증 실패 시 저장하지 않는다.
     *
     * 예외:
     * - 품목 없음: ItemNotFoundException (저장 전 중단)
     * - 비활성 품목 수정: InactiveItemCannotBeModifiedException (저장 전 중단)
     * - 잘못된 대분류/중분류: InvalidItemRequestException (저장 전 중단)
     */
    @Transactional
    public ItemView updateSelection(UpdateItemSelectionCommand command) {
        UpdateItemSelectionCommand validatedCommand = Objects.requireNonNull(command, "command");
        Item item = getBySku(validatedCommand.sku());
        if (!item.isActive()) {
            throw new InactiveItemCannotBeModifiedException(item.getSku());
        }
        validateCategorySelection(validatedCommand.categoryCode(), validatedCommand.subCategoryCode());

        item.update(
                validatedCommand.name(),
                validatedCommand.subCategoryCode(),
                validatedCommand.unit(),
                validatedCommand.safetyStock(),
                validatedCommand.unitPrice(),
                clock.instant()
        );
        Item saved = itemRepository.save(item);
        return findViewBySku(saved.getSku());
    }

    /**
     * 품목을 활성 상태로 전환한다.
     *
     * 흐름:
     * 1) SKU로 기존 품목을 조회한다.
     * 2) 이미 활성 상태인지 도메인 모델이 검증한다.
     * 3) Item 도메인 모델의 활성 상태를 변경하고 저장한다.
     *
     * 트랜잭션: 쓰기. 품목 없음 또는 이미 활성 상태면 저장하지 않는다.
     *
     * 예외:
     * - 품목 없음: ItemNotFoundException (저장 전 중단)
     * - 이미 활성 상태: InvalidItemStatusException (저장 전 중단)
     */
    @Transactional
    public Item activate(String sku) {
        Item item = getBySku(sku);

        item.activate(clock.instant());
        return itemRepository.save(item);
    }

    /**
     * 품목을 비활성 상태로 전환한다.
     *
     * 흐름:
     * 1) SKU로 기존 품목을 조회한다.
     * 2) 이미 비활성 상태인지 도메인 모델이 검증한다.
     * 3) Item 도메인 모델의 비활성 상태를 변경하고 저장한다.
     *
     * 트랜잭션: 쓰기. 품목 없음 또는 이미 비활성 상태면 저장하지 않는다.
     *
     * 예외:
     * - 품목 없음: ItemNotFoundException (저장 전 중단)
     * - 이미 비활성 상태: InvalidItemStatusException (저장 전 중단)
     */
    @Transactional
    public Item deactivate(String sku) {
        Item item = getBySku(sku);

        item.deactivate(clock.instant());
        return itemRepository.save(item);
    }

    /**
     * 지원 단위 목록을 조회한다.
     *
     * 흐름:
     * 1) ItemUnit enum 값을 코드 순서대로 반환한다.
     *
     * 트랜잭션: 읽기. 저장이나 상태 변경은 수행하지 않는다.
     *
     * 예외: 없음.
     */
    @Transactional(readOnly = true)
    public List<ItemUnit> getUnits() {
        return List.of(ItemUnit.values());
    }

    private void validateActiveCategory(String categoryCode) {
        String normalizedCategoryCode = requireText(categoryCode, "categoryCode");
        if (!itemCategoryRepository.existsActiveByCode(normalizedCategoryCode)) {
            throw new UnavailableItemCategoryException(normalizedCategoryCode);
        }
    }

    private List<String> resolveCategoryCodes(String categoryCode) {
        if (categoryCode == null) {
            return List.of();
        }

        ItemCategory category = itemCategoryRepository.findActiveByCode(categoryCode)
                .orElseThrow(() -> new CategoryNotFoundException(categoryCode));
        if (category.getDepth() == ItemCategory.SUB_CATEGORY_DEPTH) {
            return List.of(category.getCode());
        }

        List<String> childCodes = itemCategoryRepository.findSubCategories(category.getCode()).stream()
                .map(ItemCategory::getCode)
                .toList();
        return java.util.stream.Stream.concat(java.util.stream.Stream.of(category.getCode()), childCodes.stream())
                .toList();
    }

    private void validateCategorySelection(String categoryCode, String subCategoryCode) {
        if (!itemCategoryRepository.existsActiveRootByCode(categoryCode)) {
            throw new InvalidItemRequestException(ItemErrorCode.INVALID_CATEGORY, "Invalid category: " + categoryCode);
        }
        if (!itemCategoryRepository.existsActiveSubCategoryOf(categoryCode, subCategoryCode)) {
            throw new InvalidItemRequestException(ItemErrorCode.INVALID_SUB_CATEGORY, "Invalid sub category: " + subCategoryCode);
        }
    }

    private ItemView findViewBySku(String sku) {
        return itemRepository.findViewBySku(sku)
                .orElseThrow(() -> new ItemNotFoundException(sku));
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidItemException(fieldName + " is required.");
        }
        return value.trim();
    }
}
