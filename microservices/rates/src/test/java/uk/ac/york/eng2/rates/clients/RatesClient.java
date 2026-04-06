package uk.ac.york.eng2.rates.clients;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.annotation.Client;
import uk.ac.york.eng2.rates.dto.AgileRatesResponse;

@Client("/")
public interface RatesClient {
  @Get
  AgileRatesResponse getRates();
}
