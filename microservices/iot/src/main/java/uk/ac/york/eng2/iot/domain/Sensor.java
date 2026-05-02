package uk.ac.york.eng2.iot.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.persistence.*;

import java.util.Collections;
import java.util.Set;

@Serdeable
@Entity
@Table(name = "sensor")
public class Sensor {

    @Id
    @GeneratedValue
    private Long id;

    @Column
    private String name;

    @Column
    private String type;

    @ManyToOne
    @JoinColumn(name = "room_id")
    private Room room;

    @JsonIgnore
    @OneToMany(mappedBy = "sensor")
    private Set<Reading> readings = Collections.emptySet();

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public Room getRoom() {
        return room;
    }

    public void setRoom(Room room) {
        this.room = room;
    }

    public Set<Reading> getReadings() {
        return readings;
    }

    public void setReadings(Set<Reading> readings) {
        this.readings = readings;
    }
}