package com.fallguys.itemservice.infrastructure.persistence;

import com.fallguys.itemservice.domain.Item;
import com.fallguys.itemservice.domain.ItemRepository;
import com.fallguys.itemservice.domain.ItemSortBy;
import com.fallguys.itemservice.domain.ItemView;
import com.fallguys.itemservice.domain.PageResult;
import com.fallguys.itemservice.domain.SearchItemViewsQuery;
import com.fallguys.itemservice.domain.SearchItemsQuery;
import com.fallguys.itemservice.domain.SortDirection;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
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
    private final EntityManager entityManager;

    public ItemRepositoryAdapter(ItemJpaDao jpaDao, EntityManager entityManager) {
        this.jpaDao = jpaDao;
        this.entityManager = entityManager;
    }

    @Override
    public Optional<Item> findBySku(String sku) {
        return jpaDao.findById(sku).map(ItemEntity::toDomain);
    }

    @Override
    public List<Item> findBySkus(List<String> skus) {
        if (skus.isEmpty()) {
            return List.of();
        }
        return jpaDao.findAllById(skus).stream()
                .map(ItemEntity::toDomain)
                .toList();
    }

    @Override
    public Optional<ItemView> findViewBySku(String sku) {
        List<ItemView> result = entityManager.createQuery("""
                        select new com.fallguys.itemservice.domain.ItemView(
                            i.sku,
                            i.name,
                            c.code,
                            c.name,
                            p.code,
                            p.name,
                            i.unit,
                            i.safetyStock,
                            i.unitPrice,
                            i.active,
                            i.createdAt,
                            i.updatedAt
                        )
                        from ItemEntity i
                        join ItemCategoryEntity c on c.code = i.categoryCode
                        left join ItemCategoryEntity p on p.code = c.parentCode
                        where i.sku = :sku
                        """, ItemView.class)
                .setParameter("sku", sku)
                .getResultList();

        return result.stream().findFirst();
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
    public PageResult<ItemView> searchViews(SearchItemViewsQuery query) {
        String whereClause = buildViewWhereClause(query);
        String orderByClause = " order by i." + toProperty(query.sortBy()) + " " + toDirection(query.sortDirection());
        String viewQuery = """
                select new com.fallguys.itemservice.domain.ItemView(
                    i.sku,
                    i.name,
                    c.code,
                    c.name,
                    p.code,
                    p.name,
                    i.unit,
                    i.safetyStock,
                    i.unitPrice,
                    i.active,
                    i.createdAt,
                    i.updatedAt
                )
                from ItemEntity i
                join ItemCategoryEntity c on c.code = i.categoryCode
                left join ItemCategoryEntity p on p.code = c.parentCode
                """ + whereClause + orderByClause;
        String countQuery = """
                select count(i)
                from ItemEntity i
                join ItemCategoryEntity c on c.code = i.categoryCode
                left join ItemCategoryEntity p on p.code = c.parentCode
                """ + whereClause;

        TypedQuery<ItemView> typedViewQuery = entityManager.createQuery(viewQuery, ItemView.class)
                .setFirstResult(query.page() * query.size())
                .setMaxResults(query.size());
        TypedQuery<Long> typedCountQuery = entityManager.createQuery(countQuery, Long.class);
        bindViewParameters(typedViewQuery, query);
        bindViewParameters(typedCountQuery, query);

        return new PageResult<>(typedViewQuery.getResultList(), query.page(), query.size(), typedCountQuery.getSingleResult());
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
            case SAFETY_STOCK -> "safetyStock";
            case CREATED_AT -> "createdAt";
            case UPDATED_AT -> "updatedAt";
            case UNIT_PRICE -> "unitPrice";
        };
    }

    private static String buildViewWhereClause(SearchItemViewsQuery query) {
        List<String> conditions = new ArrayList<>();
        if (query.search() != null) {
            conditions.add("(lower(i.sku) like :keyword or lower(i.name) like :keyword)");
        }
        if (!query.categoryCodes().isEmpty()) {
            conditions.add("i.categoryCode in :categoryCodes");
        }
        if (query.active() != null) {
            conditions.add("i.active = :active");
        }
        if (conditions.isEmpty()) {
            return "";
        }
        return " where " + String.join(" and ", conditions);
    }

    private static void bindViewParameters(TypedQuery<?> queryObject, SearchItemViewsQuery query) {
        if (query.search() != null) {
            queryObject.setParameter("keyword", "%" + query.search().toLowerCase() + "%");
        }
        if (!query.categoryCodes().isEmpty()) {
            queryObject.setParameter("categoryCodes", query.categoryCodes());
        }
        if (query.active() != null) {
            queryObject.setParameter("active", query.active());
        }
    }

    private static String toDirection(SortDirection sortDirection) {
        return sortDirection == SortDirection.DESC ? "desc" : "asc";
    }
}
