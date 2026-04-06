create table reading (
    id bigint primary key not null,
    sensor_id bigint not null references sensor (id),
    taken_at timestamp not null,
    value double not null,
    index idx_sensor_time (sensor_id, taken_at)
);