create table topic_slot
(
    id              bigint primary key not null,

    # Kafka topic name + slot name (i.e. field)
    topic_name      varchar(255)       not null,
    slot_name       varchar(255)       not null,
    constraint uk_topic_slot unique (topic_name, slot_name),

    # Type of the value (declared by the component in advance)
    slot_type       varchar(255) check (
        slot_type = 'timestamp'
            or slot_type = 'text'
            or slot_type = 'double'
            or slot_type = 'long'
        ),

    # Typed value (text can be used to hold arbitrary JSON content)
    timestamp_value timestamp,
    text_value      longtext,
    double_value    double,
    long_value      bigint,
    constraint ck_has_value check (
        (slot_type = 'timestamp' and timestamp_value is not null)
            or (slot_type = 'text' and text_value is not null)
            or (slot_type = 'double' and double_value is not null)
            or (slot_type = 'long' and long_value is not null)
        ),
    constraint ck_has_one_value check (
        timestamp_value is not null
            xor text_value is not null
            xor double_value is not null
            xor long_value is not null
        )
);