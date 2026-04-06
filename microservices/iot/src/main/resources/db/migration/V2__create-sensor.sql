create table sensor (
  id bigint primary key not null,
  name varchar(255) unique not null,
  type varchar(255) not null,
  room_id bigint not null references room (id)
);

insert into sensor (id, name, type, room_id)
select next value for hibernate_sequence, 'Bedroom Temperature', 'temperature', id
from room where name = 'Bedroom';

insert into sensor (id, name, type, room_id)
select next value for hibernate_sequence, 'Living Room Temperature', 'temperature', id
from room where name = 'Living Room';
