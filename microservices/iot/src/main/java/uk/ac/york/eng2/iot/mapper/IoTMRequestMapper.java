package uk.ac.york.eng2.iot.mapper;

import uk.ac.york.eng2.iot.domain.Actuator;
import uk.ac.york.eng2.iot.domain.Room;
import uk.ac.york.eng2.iot.domain.Sensor;
import uk.ac.york.eng2.iot.dto.ActuatorCreateDTO;
import uk.ac.york.eng2.iot.dto.ActuatorUpdateDTO;
import uk.ac.york.eng2.iot.dto.RoomCreateDTO;
import uk.ac.york.eng2.iot.dto.RoomUpdateDTO;
import uk.ac.york.eng2.iot.dto.SensorCreateDTO;
import uk.ac.york.eng2.iot.dto.SensorUpdateDTO;

public class IoTMRequestMapper {

    private IoTMRequestMapper() {
    }

    public static void apply(RoomCreateDTO dto, Room room) {
        if (dto == null || room == null) {
            return;
        }
        room.setName(dto.getName());
    }

    public static void apply(RoomUpdateDTO dto, Room room) {
        if (dto == null || room == null) {
            return;
        }
        room.setName(dto.getName());
    }

    public static void apply(SensorCreateDTO dto, Sensor sensor, Room room) {
        if (dto == null || sensor == null) {
            return;
        }
        sensor.setName(dto.getName());
        sensor.setType(dto.getType());
        sensor.setRoom(room);
    }

    public static void apply(SensorUpdateDTO dto, Sensor sensor, Room room) {
        if (dto == null || sensor == null) {
            return;
        }
        sensor.setName(dto.getName());
        sensor.setType(dto.getType());
        sensor.setRoom(room);
    }

    public static void apply(ActuatorCreateDTO dto, Actuator actuator, Room room) {
        if (dto == null || actuator == null) {
            return;
        }
        actuator.setName(dto.getName());
        actuator.setType(dto.getType());
        actuator.setTargetState(dto.getTargetState());
        actuator.setRoom(room);
    }

    public static void apply(ActuatorUpdateDTO dto, Actuator actuator, Room room) {
        if (dto == null || actuator == null) {
            return;
        }
        actuator.setName(dto.getName());
        actuator.setType(dto.getType());
        actuator.setTargetState(dto.getTargetState());
        actuator.setRoom(room);
    }
}
