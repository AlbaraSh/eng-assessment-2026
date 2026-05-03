package uk.ac.york.eng2.reactive.components.agilerates;

import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import uk.ac.york.cs.eng2.reactive.rates.api.RatesApi;
import uk.ac.york.cs.eng2.reactive.rates.model.AgileRatesResponse;

@Singleton
@Requires(notEnv = Environment.TEST)
public class AgileRatesGateway {

    @Inject
    private RatesApi ratesApi;

    public AgileRatesResponse getRates() {
        return ratesApi.getRates();
    }
}
