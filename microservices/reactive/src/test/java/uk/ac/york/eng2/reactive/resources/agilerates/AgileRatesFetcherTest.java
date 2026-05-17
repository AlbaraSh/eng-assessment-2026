package uk.ac.york.eng2.reactive.resources.agilerates;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.york.eng2.reactive.components.agilerates.AgileRatesFetcher;
import uk.ac.york.eng2.reactive.components.agilerates.AgileRatesFetcherLogic;

import java.lang.reflect.Field;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class AgileRatesFetcherTest {

    private AgileRatesFetcher fetcher;
    private AgileRatesFetcherLogic logic;

    @BeforeEach
    public void setUp() {
        fetcher = new AgileRatesFetcher();
        logic = mock(AgileRatesFetcherLogic.class);

        setField(fetcher, "logic", logic);
    }

    @Test
    public void fetchEvery12HoursDelegatesToLogic() {
        fetcher.fetchEvery12Hours();

        verify(logic).tryFetch();
    }

    @Test
    public void retryUntilAvailableDelegatesToLogic() {
        fetcher.retryUntilAvailable();

        verify(logic).retryUntilAvailable();
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
