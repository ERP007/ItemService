package com.fallguys.itemservice.infrastructure.persistence;

import com.fallguys.itemservice.domain.Item;
import com.fallguys.itemservice.domain.ItemRepository;
import com.fallguys.itemservice.domain.ItemSortBy;
import com.fallguys.itemservice.domain.PageResult;
import com.fallguys.itemservice.domain.SearchItemsQuery;
import com.fallguys.itemservice.domain.SortDirection;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class ItemRepositoryAdapter implements ItemRepository {

    private final ItemJpaDao jpaDao;

    public ItemRepositoryAdapter(ItemJpaDao jpaDao) {
        this.jpaDao = jpaDao;
    }

    @Override
    public Optional<Item> findBySku(String sku) {
        return jpaDao.findById(sku).map(ItemEntity::toDomain);
    }

    @Override
    public boolean existsBySku(String sku) {
        return jpaDao.existsById(sku);
    }

    @Override
    public PageResult<Item> search(SearchItemsQuery query) {
        PageRequest pageRequest = PageRequest.of(query.page(), query.size(), toSort(query.sortBy(), query.sortDirection()));
        Page<ItemEntity> page = jpaDao.findAll(toSpecification(query), pageRequest);

        return new PageResult<>(
                page.getContent().stream()
                        .map(ItemEntity::toDomain)
                        .toList(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }

    @Override
    public Item save(Item item) {
        ItemEntity entity = jpaDao.findById(item.getSku())
                .map(existing -> existing.update(item))
                .orElseGet(() -> ItemEntity.from(item));
        return jpaDao.save(entity).toDomain();
    }

    private static Specification<ItemEntity> toSpecification(SearchItemsQuery query) {
        return (root, criteriaQuery, criteriaBuilder) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (query.search() != null) {
                String keyword = "%" + query.search().toLowerCase() + "%";
                predicates.add(criteriaBuilder.or(
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("sku")), keyword),
                        criteriaBuilder.like(criteriaBuilder.lower(root.get("name")), keyword)
                ));
            }
            if (query.categoryCode() != null) {
                predicates.add(criteriaBuilder.equal(root.get("categoryCode"), query.categoryCode()));
            }
            if (query.active() != null) {
                predicates.add(criteriaBuilder.equal(root.get("active"), query.active()));
            }

            return criteriaBuilder.and(predicates.toArray(Predicate[]::new));
        };
    }

    private static Sort toSort(ItemSortBy sortBy, SortDirection sortDirection) {
        Sort.Direction direction = sortDirection == SortDirection.DESC ? Sort.Direction.DESC : Sort.Direction.ASC;
        return Sort.by(direction, toProperty(sortBy));
    }

    private static String toProperty(ItemSortBy sortBy) {
        return switch (sortBy) {
            case SKU -> "sku";
            case NAME -> "name";
            case CREATED_AT -> "createdAt";
            case UPDATED_AT -> "updatedAt";
            case UNIT_PRICE -> "unitPrice";
        };
    }
}
