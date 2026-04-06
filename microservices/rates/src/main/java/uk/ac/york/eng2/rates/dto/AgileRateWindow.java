package uk.ac.york.eng2.rates.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;

@Serdeable
public class AgileRateWindow {
  private Instant startTime, endTime;
  private double averageRate;
  private boolean prediction;

  public Instant getStartTime() {
    return startTime;
  }

  public void setStartTime(Instant startTime) {
    this.startTime = startTime;
  }

  public Instant getEndTime() {
    return endTime;
  }

  public void setEndTime(Instant endTime) {
    this.endTime = endTime;
  }

  public double getAverageRate() {
    return averageRate;
  }

  public void setAverageRate(double averageRate) {
    this.averageRate = averageRate;
  }

  public boolean isPrediction() {
    return prediction;
  }

  public void setPrediction(boolean prediction) {
    this.prediction = prediction;
  }
}
