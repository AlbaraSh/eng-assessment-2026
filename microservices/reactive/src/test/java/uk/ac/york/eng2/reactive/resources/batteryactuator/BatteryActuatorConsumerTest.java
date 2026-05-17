package uk.ac.york.eng2.reactive.resources.batteryactuator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.york.eng2.reactive.components.BatteryDecisionSnapshot;
import uk.ac.york.eng2.reactive.components.batteryactuator.BatteryActuatorConsumer;
import uk.ac.york.eng2.reactive.components.batteryactuator.BatteryActuatorLogic;

import java.lang.reflect.Field;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

public class BatteryActuatorConsumerTest {

    private BatteryActuatorConsumer consumer;
    private BatteryActuatorLogic logic;

    @BeforeEach
    public void setUp() {
        consumer = new BatteryActuatorConsumer();
        logic = mock(BatteryActuatorLogic.class);

        setField(consumer, "logic", logic);
    }

    @Test
    public void receiveDelegatesToLogic() {
        BatteryDecisionSnapshot snapshot = new BatteryDecisionSnapshot();
        snapshot.setTargetState("export_to_grid");

        consumer.receive(snapshot);

        verify(logic).receive(snapshot);
    }

    @Test
    public void reapplyLatestDecisionIfNeededDelegatesToLogic() {
        consumer.reapplyLatestDecisionIfNeeded();

        verify(logic).reapplyLatestDecisionIfNeeded();
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