package uk.ac.york.eng2.reactive.resources.importrate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.york.eng2.reactive.components.AgileRatesSnapshot;
import uk.ac.york.eng2.reactive.components.importrate.CurrentImportRateConsumer;
import uk.ac.york.eng2.reactive.components.importrate.CurrentImportRateLogic;

import java.lang.reflect.Field;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CurrentImportRateConsumerTest {

    private CurrentImportRateConsumer consumer;
    private CurrentImportRateLogic logic;

    @BeforeEach
    public void setUp() {
        consumer = new CurrentImportRateConsumer();
        logic = mock(CurrentImportRateLogic.class);

        setField(consumer, "logic", logic);
    }

    @Test
    public void receiveDelegatesToLogic() {
        AgileRatesSnapshot snapshot = new AgileRatesSnapshot();
        snapshot.setJson("{json}");

        consumer.receive(snapshot);

        verify(logic).receive(snapshot);
    }

    @Test
    public void recomputeEvery30MinutesDelegatesToLogic() {
        consumer.recomputeEvery30Minutes();

        verify(logic).recomputeEvery30Minutes();
    }

    @Test
    public void retryUntilCurrentImportRateAvailableDelegatesToLogic() {
        consumer.retryUntilCurrentImportRateAvailable();

        verify(logic).retryUntilCurrentImportRateAvailable();
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