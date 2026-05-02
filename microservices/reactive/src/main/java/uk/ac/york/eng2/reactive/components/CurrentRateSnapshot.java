package uk.ac.york.eng2.reactive.components;

import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;

@Serdeable
public class CurrentRateSnapshot {
    private Double rate;
    private Instant atTime;

    public Double getRate() {
        return rate;
    }

    public void setRate(Double rate) {
        this.rate = rate;
    }

    public Instant getAtTime() {
        return atTime;
    }

    public void setAtTime(Instant atTime) {
        this.atTime = atTime;
    }
}