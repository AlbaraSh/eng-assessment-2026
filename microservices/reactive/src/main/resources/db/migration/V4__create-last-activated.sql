# This table is only needed for the 'tolerances' requirement
create table last_activated (
    id bigint primary key not null,
    component_id bigint not null references component (id),
    topic_slot_id bigint not null references topic_slot (id),
    constraint uk_component_topic unique (component_id, topic_slot_id),

    # Value the slot had when the component was last activated
    # (simplified to only double as needed by example).
    activated_value double not null
);
