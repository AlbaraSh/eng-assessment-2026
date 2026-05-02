package uk.ac.york.eng2.iot.dto;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class ActuatorUpdateDTO {
    private String name;
    private String type;
    private Long roomId;
    private String targetState;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Long getRoomId() {
        return roomId;
    }

    public void setRoomId(Long roomId) {
        this.roomId = roomId;
    }

    public String getTargetState() {
        return targetState;
    }

    public void setTargetState(String targetState) {
        this.targetState = targetState;
    }
}
