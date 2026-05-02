package uk.ac.york.eng2.iot.mapper;

import uk.ac.york.eng2.iot.domain.Actuator;
import uk.ac.york.eng2.iot.domain.Reading;
import uk.ac.york.eng2.iot.domain.Room;
import uk.ac.york.eng2.iot.domain.Sensor;
import uk.ac.york.eng2.iot.dto.ActuatorDTO;
import uk.ac.york.eng2.iot.dto.ReadingDTO;
import uk.ac.york.eng2.iot.dto.RoomDTO;
import uk.ac.york.eng2.iot.dto.SensorDTO;

public class IoTMMapper {

    private IoTMMapper() {
    }

    public static RoomDTO toDTO(Room room) {
        if (room == null) {
            return null;
        }

        RoomDTO dto = new RoomDTO();
        dto.setId(room.getId());
        dto.setName(room.getName());
        return dto;
    }

    public static SensorDTO toDTO(Sensor sensor) {
        if (sensor == null) {
            return null;
        }

        SensorDTO dto = new SensorDTO();
        dto.setId(sensor.getId());
        dto.setName(sensor.getName());
        dto.setType(sensor.getType());

        if (sensor.getRoom() != null) {
            dto.setRoomId(sensor.getRoom().getId());
        }

        return dto;
    }

    public static ActuatorDTO toDTO(Actuator actuator) {
        if (actuator == null) {
            return null;
        }

        ActuatorDTO dto = new ActuatorDTO();
        dto.setId(actuator.getId());
        dto.setName(actuator.getName());
        dto.setType(actuator.getType());
        dto.setTargetState(actuator.getTargetState());

        if (actuator.getRoom() != null) {
            dto.setRoomId(actuator.getRoom().getId());
        }

        return dto;
    }

    public static ReadingDTO toDTO(Reading reading) {
        if (reading == null) {
            return null;
        }

        ReadingDTO dto = new ReadingDTO();
        dto.setId(reading.getId());
        dto.setTakenAt(reading.getTakenAt());
        dto.setValue(reading.getValue());

        if (reading.getSensor() != null) {
            dto.setSensorId(reading.getSensor().getId());
        }

        return dto;
    }
}