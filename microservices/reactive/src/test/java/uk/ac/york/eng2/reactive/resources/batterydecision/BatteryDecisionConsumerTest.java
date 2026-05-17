package uk.ac.york.eng2.reactive.resources.batterydecision;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.york.eng2.reactive.components.CurrentRateSnapshot;
import uk.ac.york.eng2.reactive.components.batterydecision.BatteryDecisionConsumer;
import uk.ac.york.eng2.reactive.components.batterydecision.BatteryDecisionLogic;

import java.lang.reflect.Field;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class BatteryDecisionConsumerTest {

    private BatteryDecisionConsumer consumer;
    private BatteryDecisionLogic logic;

    @BeforeEach
    public void setUp() {
        consumer = new BatteryDecisionConsumer();
        logic = mock(BatteryDecisionLogic.class);

        setField(consumer, "logic", logic);
    }

    @Test
    public void receiveImportRateDelegatesToLogic() {
        CurrentRateSnapshot snapshot = new CurrentRateSnapshot();
        snapshot.setRate(7.0);

        consumer.receiveImportRate(snapshot);

        verify(logic).receiveImportRate(snapshot);
    }

    @Test
    public void receiveExportRateDelegatesToLogic() {
        CurrentRateSnapshot snapshot = new CurrentRateSnapshot();
        snapshot.setRate(18.0);

        consumer.receiveExportRate(snapshot);

        verify(logic).receiveExportRate(snapshot);
    }

    @Test
    public void retryUntilBatteryDecisionAvailableDelegatesToLogic() {
        consumer.retryUntilBatteryDecisionAvailable();

        verify(logic).retryUntilBatteryDecisionAvailable();
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