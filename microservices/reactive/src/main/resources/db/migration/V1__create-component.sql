create sequence hibernate_sequence;

create table component
(
    id   bigint primary key  not null,
    name varchar(255) unique not null
);