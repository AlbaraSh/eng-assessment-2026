package uk.ac.york.eng2.rates.dto;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class AgileRateResult {
  private String source;
  private boolean prediction;
  private double rate;

  public String getSource() {
    return source;
  }

  public void setSource(String source) {
    this.source = source;
  }

  public boolean isPrediction() {
    return prediction;
  }

  public void setPrediction(boolean prediction) {
    this.prediction = prediction;
  }

  public double getRate() {
    return rate;
  }

  public void setRate(double rate) {
    this.rate = rate;
  }

  @Override
  public String toString() {
    return "AgileRateResult{" +
        "source='" + source + '\'' +
        ", prediction=" + prediction +
        ", rate=" + rate +
        '}';
  }
}
