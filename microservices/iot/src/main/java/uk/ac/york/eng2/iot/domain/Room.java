package uk.ac.york.eng2.iot.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.micronaut.serde.annotation.Serdeable;
import jakarta.persistence.*;

import java.util.Collections;
import java.util.Set;

@Serdeable
@Entity
@Table(name = "room")
public class Room {

    @Id
    @GeneratedValue
    private Long id;

    @Column
    private String name;

    @JsonIgnore
    @OneToMany(mappedBy = "room")
    private Set<Sensor> sensors = Collections.emptySet();

    @JsonIgnore
    @OneToMany(mappedBy = "room")
    private Set<Actuator> actuators = Collections.emptySet();

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

    public Set<Sensor> getSensors() {
        return sensors;
    }

    public void setSensors(Set<Sensor> sensors) {
        this.sensors = sensors;
    }

    public Set<Actuator> getActuators() {
        return actuators;
    }

    public void setActuators(Set<Actuator> actuators) {
        this.actuators = actuators;
    }
}