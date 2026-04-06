package uk.ac.york.eng2.rates.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Serdeable
public class AgileRatesResponse {
  private String result;
  private List<AgileSlotRates> rates = new ArrayList<>();
  private Map<Instant, Map<Integer, AgileRateWindow>> cheapestWindowsPerSlot = new TreeMap<>();

  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public List<AgileSlotRates> getRates() {
    return rates;
  }

  public void setRates(List<AgileSlotRates> rates) {
    this.rates = rates;
  }

  public Map<Instant, Map<Integer, AgileRateWindow>> getCheapestWindowsPerSlot() {
    return cheapestWindowsPerSlot;
  }

  public void setCheapestWindowsPerSlot(Map<Instant, Map<Integer, AgileRateWindow>> cheapestWindowsPerSlot) {
    this.cheapestWindowsPerSlot = cheapestWindowsPerSlot;
  }

  @Override
  public String toString() {
    return "RatesResponse{" +
        "result='" + result + '\'' +
        ", rates=" + rates +
        ", cheapestWindowsPerSlot=" + cheapestWindowsPerSlot +
        '}';
  }
}
