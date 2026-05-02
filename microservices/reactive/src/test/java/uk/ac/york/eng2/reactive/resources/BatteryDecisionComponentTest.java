package uk.ac.york.eng2.reactive.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.ac.york.eng2.reactive.components.BatteryDecisionComponent;
import uk.ac.york.eng2.reactive.components.BatteryDecisionSnapshot;
import uk.ac.york.eng2.reactive.components.CurrentRateSnapshot;
import uk.ac.york.eng2.reactive.kafka.BatteryDecisionProducer;
import uk.ac.york.eng2.reactive.service.BatteryDecisionService;
import uk.ac.york.eng2.reactive.service.ComponentService;
import uk.ac.york.eng2.reactive.service.TopicSlotService;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class BatteryDecisionComponentTest {

    private BatteryDecisionComponent component;
    private ComponentService componentService;
    private TopicSlotService topicSlotService;
    private BatteryDecisionService batteryDecisionService;
    private BatteryDecisionProducer producer;

    @BeforeEach
    public void setUp() {
        component = new BatteryDecisionComponent();
        componentService = mock(ComponentService.class);
        topicSlotService = mock(TopicSlotService.class);
        batteryDecisionService = mock(BatteryDecisionService.class);
        producer = mock(BatteryDecisionProducer.class);

        setField(component, "componentService", componentService);
        setField(component, "topicSlotService", topicSlotService);
        setField(component, "batteryDecisionService", batteryDecisionService);
        setField(component, "producer", producer);
    }

    @Test
    public void registerEnsuresComponentExists() {
        component.register();
        verify(componentService).ensureExists("BatteryDecisionComponent");
    }

    @Test
    public void receiveImportPublishesDecisionWhenInputsExist() {
        when(topicSlotService.getDoubleValue("current-import-rate", "rate")).thenReturn(Optional.of(7.0));
        when(topicSlotService.getDoubleValue("current-export-rate", "rate")).thenReturn(Optional.of(18.0));
        when(batteryDecisionService.decideTargetState(7.0, 18.0)).thenReturn("import_to_100");

        CurrentRateSnapshot snapshot = new CurrentRateSnapshot();
        snapshot.setRate(7.0);

        component.receiveImport("latest", snapshot);

        verify(topicSlotService).saveTextSlot("battery-decision", "targetState", "import_to_100");
        verify(topicSlotService).saveTimestampSlot(eq("battery-decision"), eq("atTime"), any());

        ArgumentCaptor<BatteryDecisionSnapshot> captor = ArgumentCaptor.forClass(BatteryDecisionSnapshot.class);
        verify(producer).send(eq("latest"), captor.capture());

        BatteryDecisionSnapshot published = captor.getValue();
        assertEquals("import_to_100", published.getTargetState());
    }

    @Test
    public void receiveExportDoesNothingWhenOneInputMissing() {
        when(topicSlotService.getDoubleValue("current-import-rate", "rate")).thenReturn(Optional.empty());
        when(topicSlotService.getDoubleValue("current-export-rate", "rate")).thenReturn(Optional.of(18.0));

        CurrentRateSnapshot snapshot = new CurrentRateSnapshot();
        snapshot.setRate(18.0);

        component.receiveExport("latest", snapshot);

        verify(topicSlotService, never()).saveTextSlot(eq("battery-decision"), eq("targetState"), any());
        verifyNoInteractions(producer);
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