package uk.ac.york.eng2.reactive.components;

import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;

@Serdeable
public class AgileRatesSnapshot {
    private String json;
    private Instant validFrom;
    private Instant validTo;

    public String getJson() {
        return json;
    }

    public void setJson(String json) {
        this.json = json;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(Instant validFrom) {
        this.validFrom = validFrom;
    }

    public Instant getValidTo() {
        return validTo;
    }

    public void setValidTo(Instant validTo) {
        this.validTo = validTo;
    }
}