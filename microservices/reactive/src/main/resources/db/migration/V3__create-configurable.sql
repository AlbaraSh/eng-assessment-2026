create table configurable (
    id bigint primary key not null,
    component_id bigint not null references component (id),
    name varchar(255) not null,
    constraint uk_component_name unique (component_id, name),

    # For the example, doubles are enough, so we keep it simple
    value double not null
);