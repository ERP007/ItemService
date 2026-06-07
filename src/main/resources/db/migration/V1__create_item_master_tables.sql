create table item_categories (
    code varchar(64) primary key,
    name varchar(100) not null,
    parent_code varchar(64),
    depth integer not null,
    display_order integer not null,
    active boolean not null,
    constraint fk_item_categories_parent
        foreign key (parent_code) references item_categories (code),
    constraint chk_item_categories_depth
        check (depth in (1, 2)),
    constraint chk_item_categories_parent_by_depth
        check (
            (depth = 1 and parent_code is null)
            or (depth = 2 and parent_code is not null)
        ),
    constraint chk_item_categories_display_order
        check (display_order >= 0)
);

create index idx_item_categories_parent_active_order
    on item_categories (parent_code, active, display_order);

create table items (
    sku varchar(64) primary key,
    name varchar(255) not null,
    category_code varchar(64) not null,
    unit varchar(16) not null,
    safety_stock integer not null,
    unit_price integer not null,
    active boolean not null,
    created_at timestamp(6) with time zone not null,
    updated_at timestamp(6) with time zone not null,
    constraint fk_items_category
        foreign key (category_code) references item_categories (code),
    constraint chk_items_safety_stock
        check (safety_stock >= 0),
    constraint chk_items_unit_price
        check (unit_price >= 0)
);

create index idx_items_category_active
    on items (category_code, active);

create index idx_items_active_name
    on items (active, name);
