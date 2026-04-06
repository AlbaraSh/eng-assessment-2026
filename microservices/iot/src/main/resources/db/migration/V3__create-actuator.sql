create table actuator (
    id bigint primary key not null,
    name varchar(255) unique not null,
    type varchar(255) not null,
    room_id bigint not null references room (id),

    # actual string value will be interpreted based on type:
    #
    # - 'temperature' actuators can have 'off' or the target temperature (as integer)
    # - 'battery' actuators can have 'sell_excess', 'export_to INTEGER', or 'import_to INTEGER'
    target_state varchar(255) not null
);

insert into actuator (id, name, type, room_id, target_state)
select next value for hibernate_sequence, 'Bedroom Heater', 'heater', id, 'off'
from room where name = 'Bedroom';

insert into actuator (id, name, type, room_id, target_state)
select next value for hibernate_sequence, 'Living Room Heater', 'heater', id, 'off'
from room where name = 'Living Room';

insert into room (id, name) values (next value for hibernate_sequence, 'Garage');

insert into actuator (id, name, type, room_id, target_state)
select next value for hibernate_sequence, 'Home Battery', 'battery', id, 'sell_excess'
from room where name = 'Garage';