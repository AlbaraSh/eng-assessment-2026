package uk.ac.york.eng2.reactive.components;

import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;

@Serdeable
public class BatteryDecisionSnapshot {
    private String targetState;
    private Instant atTime;

    public String getTargetState() {
        return targetState;
    }

    public void setTargetState(String targetState) {
        this.targetState = targetState;
    }

    public Instant getAtTime() {
        return atTime;
    }

    public void setAtTime(Instant atTime) {
        this.atTime = atTime;
    }
}