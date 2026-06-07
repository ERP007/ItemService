package com.fallguys.itemservice.domain;

import com.fallguys.itemservice.domain.exception.DuplicateItemSkuException;
import com.fallguys.itemservice.domain.exception.InvalidItemException;
import com.fallguys.itemservice.domain.exception.ItemNotFoundException;
import com.fallguys.itemservice.domain.exception.UnavailableItemCategoryException;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class ItemService {

    private final ItemRepository itemRepository;
    private final ItemCategoryRepository itemCategoryRepository;
    private final Clock clock;

    public ItemService(ItemRepository itemRepository, ItemCategoryRepository itemCategoryRepository) {
        this(itemRepository, itemCategoryRepository, Clock.systemUTC());
    }

    public ItemService(ItemRepository itemRepository, ItemCategoryRepository itemCategoryRepository, Clock clock) {
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
    public PageResult<Item> search(SearchItemsQuery query) {
        SearchItemsQuery validatedQuery = Objects.requireNonNull(query, "query");

        return itemRepository.search(validatedQuery);
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
    public Item getBySku(String sku) {
        String normalizedSku = requireText(sku, "sku");

        return itemRepository.findBySku(normalizedSku)
                .orElseThrow(() -> new ItemNotFoundException(normalizedSku));
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
     * 품목을 활성 상태로 전환한다.
     *
     * 흐름:
     * 1) SKU로 기존 품목을 조회한다.
     * 2) Item 도메인 모델의 활성 상태를 변경하고 저장한다.
     *
     * 트랜잭션: 쓰기. 품목 없음 시 저장하지 않는다.
     *
     * 예외:
     * - 품목 없음: ItemNotFoundException (저장 전 중단)
     */
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
     * 2) Item 도메인 모델의 비활성 상태를 변경하고 저장한다.
     *
     * 트랜잭션: 쓰기. 품목 없음 시 저장하지 않는다.
     *
     * 예외:
     * - 품목 없음: ItemNotFoundException (저장 전 중단)
     */
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
    public List<ItemUnit> getUnits() {
        return List.of(ItemUnit.values());
    }

    private void validateActiveCategory(String categoryCode) {
        String normalizedCategoryCode = requireText(categoryCode, "categoryCode");
        if (!itemCategoryRepository.existsActiveByCode(normalizedCategoryCode)) {
            throw new UnavailableItemCategoryException(normalizedCategoryCode);
        }
    }

    private static String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new InvalidItemException(fieldName + " is required.");
        }
        return value.trim();
    }
}
