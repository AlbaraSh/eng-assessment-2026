package uk.ac.york.eng2.rates.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;

@Serdeable
public class AgileSlotRates {
  private Instant deliveryStart;
  private Instant deliveryEnd;
  private AgileRate agileRate, agileOutgoingRate;

  public Instant getDeliveryStart() {
    return deliveryStart;
  }

  public void setDeliveryStart(Instant deliveryStart) {
    this.deliveryStart = deliveryStart;
  }

  public Instant getDeliveryEnd() {
    return deliveryEnd;
  }

  public void setDeliveryEnd(Instant deliveryEnd) {
    this.deliveryEnd = deliveryEnd;
  }

  public AgileRate getAgileRate() {
    return agileRate;
  }

  public void setAgileRate(AgileRate agileRate) {
    this.agileRate = agileRate;
  }

  public AgileRate getAgileOutgoingRate() {
    return agileOutgoingRate;
  }

  public void setAgileOutgoingRate(AgileRate agileOutgoingRate) {
    this.agileOutgoingRate = agileOutgoingRate;
  }

  @Override
  public String toString() {
    return "SlotRates{" +
        "deliveryStart=" + deliveryStart +
        ", deliveryEnd=" + deliveryEnd +
        ", agileRate=" + agileRate +
        ", agileOutgoingRate=" + agileOutgoingRate +
        '}';
  }
}
