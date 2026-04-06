package uk.ac.york.eng2.rates.resources;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import uk.ac.york.eng2.rates.clients.RatesClient;
import uk.ac.york.eng2.rates.dto.AgileRatesResponse;

@MicronautTest
public class RatesResourceTest {
  @Inject
  private RatesClient ratesClient;

  @Test
  public void testGetRates() {
    AgileRatesResponse response = ratesClient.getRates();
    assertFalse(response.getRates().isEmpty());
    assertNotNull(response.getRates().get(0).getAgileRate());
    assertNotNull(response.getRates().get(0).getAgileOutgoingRate());
    assertFalse(response.getCheapestWindowsPerSlot().isEmpty());
  }
}
