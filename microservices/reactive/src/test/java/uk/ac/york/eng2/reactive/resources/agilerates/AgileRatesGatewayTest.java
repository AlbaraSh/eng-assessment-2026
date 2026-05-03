package uk.ac.york.eng2.reactive.resources.agilerates;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.york.cs.eng2.reactive.rates.api.RatesApi;
import uk.ac.york.cs.eng2.reactive.rates.model.AgileRatesResponse;
import uk.ac.york.eng2.reactive.components.agilerates.AgileRatesGateway;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AgileRatesGatewayTest {

    private AgileRatesGateway gateway;
    private RatesApi ratesApi;

    @BeforeEach
    public void setUp() {
        gateway = new AgileRatesGateway();
        ratesApi = mock(RatesApi.class);

        setField(gateway, "ratesApi", ratesApi);
    }

    @Test
    public void getRatesDelegatesToRatesApi() {
        AgileRatesResponse response = new AgileRatesResponse();

        when(ratesApi.getRates()).thenReturn(response);

        AgileRatesResponse result = gateway.getRates();

        assertSame(response, result);
        verify(ratesApi).getRates();
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
