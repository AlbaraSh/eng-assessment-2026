package uk.ac.york.eng2.rates.dto;

import io.micronaut.serde.annotation.Serdeable;

@Serdeable
public class AgileRate {
  private AgileRateResult result;

  public AgileRateResult getResult() {
    return result;
  }

  public void setResult(AgileRateResult result) {
    this.result = result;
  }

  @Override
  public String toString() {
    return "AgileRate{" +
        "result=" + result +
        '}';
  }
}
