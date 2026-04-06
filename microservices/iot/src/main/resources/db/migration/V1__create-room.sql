create sequence hibernate_sequence;

create table room (
  id bigint primary key not null,
  name varchar(255) unique not null
);

insert into room (id, name) values
  (next value for hibernate_sequence, 'Bedroom'),
  (next value for hibernate_sequence, 'Living Room');
