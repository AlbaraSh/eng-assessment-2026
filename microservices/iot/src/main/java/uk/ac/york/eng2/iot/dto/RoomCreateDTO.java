package uk.ac.york.eng2.iot.dto;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class RoomCreateDTO {
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
