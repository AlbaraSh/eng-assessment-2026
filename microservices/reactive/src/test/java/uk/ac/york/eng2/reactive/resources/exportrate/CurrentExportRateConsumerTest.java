package uk.ac.york.eng2.reactive.resources.exportrate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.york.eng2.reactive.components.AgileRatesSnapshot;
import uk.ac.york.eng2.reactive.components.exportrate.CurrentExportRateConsumer;
import uk.ac.york.eng2.reactive.components.exportrate.CurrentExportRateLogic;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class CurrentExportRateConsumerTest {

    private CurrentExportRateConsumer consumer;
    private CurrentExportRateLogic logic;

    @BeforeEach
    public void setUp() {
        consumer = new CurrentExportRateConsumer();
        logic = mock(CurrentExportRateLogic.class);

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
    public void retryUntilCurrentExportRateAvailableDelegatesToLogic() {
        consumer.retryUntilCurrentExportRateAvailable();

        verify(logic).retryUntilCurrentExportRateAvailable();
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
