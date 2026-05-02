package uk.ac.york.eng2.iot.dto;

import io.micronaut.serde.annotation.Serdeable;
import java.time.Instant;

@Serdeable
public class ReadingDTO {
    private Long id;
    private Long sensorId;
    private Instant takenAt;
    private Double value;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSensorId() {
        return sensorId;
    }

    public void setSensorId(Long sensorId) {
        this.sensorId = sensorId;
    }

    public Instant getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(Instant takenAt) {
        this.takenAt = takenAt;
    }

    public Double getValue() {
        return value;
    }

    public void setValue(Double value) {
        this.value = value;
    }
}
