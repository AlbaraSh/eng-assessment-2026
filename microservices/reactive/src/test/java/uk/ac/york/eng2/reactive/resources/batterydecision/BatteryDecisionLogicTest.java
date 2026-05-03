package uk.ac.york.eng2.reactive.resources.batterydecision;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.ac.york.eng2.reactive.components.BatteryDecisionSnapshot;
import uk.ac.york.eng2.reactive.components.CurrentRateSnapshot;
import uk.ac.york.eng2.reactive.components.batterydecision.BatteryDecisionLogic;
import uk.ac.york.eng2.reactive.kafka.BatteryDecisionProducer;
import uk.ac.york.eng2.reactive.service.BatteryDecisionService;
import uk.ac.york.eng2.reactive.service.ComponentService;
import uk.ac.york.eng2.reactive.service.TopicSlotService;

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class BatteryDecisionLogicTest {

    private BatteryDecisionLogic logic;
    private ComponentService componentService;
    private TopicSlotService topicSlotService;
    private BatteryDecisionService batteryDecisionService;
    private BatteryDecisionProducer producer;

    @BeforeEach
    public void setUp() {
        logic = new BatteryDecisionLogic();

        componentService = mock(ComponentService.class);
        topicSlotService = mock(TopicSlotService.class);
        batteryDecisionService = mock(BatteryDecisionService.class);
        producer = mock(BatteryDecisionProducer.class);

        setField(logic, "componentService", componentService);
        setField(logic, "topicSlotService", topicSlotService);
        setField(logic, "batteryDecisionService", batteryDecisionService);
        setField(logic, "producer", producer);
    }

    @Test
    public void registerEnsuresComponentExists() {
        logic.register();

        verify(componentService).ensureExists("BatteryDecisionComponent");
    }

    @Test
    public void receiveImportRatePublishesDecisionWhenInputsExist() {
        when(topicSlotService.getDoubleValue("current-import-rate", "rate"))
                .thenReturn(Optional.of(7.0));
        when(topicSlotService.getDoubleValue("current-export-rate", "rate"))
                .thenReturn(Optional.of(18.0));
        when(batteryDecisionService.decideTargetState(7.0, 18.0))
                .thenReturn("import_to_100");

        CurrentRateSnapshot snapshot = new CurrentRateSnapshot();
        snapshot.setRate(7.0);
        snapshot.setAtTime(Instant.parse("2026-04-09T10:30:00Z"));

        logic.receiveImportRate(snapshot);

        verify(topicSlotService).saveDoubleSlot("current-import-rate", "rate", 7.0);
        verify(topicSlotService).saveTimestampSlot(
                "current-import-rate",
                "atTime",
                Instant.parse("2026-04-09T10:30:00Z")
        );

        verify(topicSlotService).saveTextSlot(
                "battery-decision",
                "targetState",
                "import_to_100"
        );
        verify(topicSlotService).saveTimestampSlot(
                eq("battery-decision"),
                eq("atTime"),
                any(Instant.class)
        );

        ArgumentCaptor<BatteryDecisionSnapshot> captor =
                ArgumentCaptor.forClass(BatteryDecisionSnapshot.class);

        verify(producer).send(eq("latest"), captor.capture());

        BatteryDecisionSnapshot published = captor.getValue();

        assertEquals("import_to_100", published.getTargetState());
        assertNotNull(published.getAtTime());
    }

    @Test
    public void receiveExportRatePublishesDecisionWhenInputsExist() {
        when(topicSlotService.getDoubleValue("current-import-rate", "rate"))
                .thenReturn(Optional.of(12.0));
        when(topicSlotService.getDoubleValue("current-export-rate", "rate"))
                .thenReturn(Optional.of(22.0));
        when(batteryDecisionService.decideTargetState(12.0, 22.0))
                .thenReturn("export_to_grid");

        CurrentRateSnapshot snapshot = new CurrentRateSnapshot();
        snapshot.setRate(22.0);
        snapshot.setAtTime(Instant.parse("2026-04-09T11:00:00Z"));

        logic.receiveExportRate(snapshot);

        verify(topicSlotService).saveDoubleSlot("current-export-rate", "rate", 22.0);
        verify(topicSlotService).saveTimestampSlot(
                "current-export-rate",
                "atTime",
                Instant.parse("2026-04-09T11:00:00Z")
        );

        verify(topicSlotService).saveTextSlot(
                "battery-decision",
                "targetState",
                "export_to_grid"
        );
        verify(topicSlotService).saveTimestampSlot(
                eq("battery-decision"),
                eq("atTime"),
                any(Instant.class)
        );

        ArgumentCaptor<BatteryDecisionSnapshot> captor =
                ArgumentCaptor.forClass(BatteryDecisionSnapshot.class);

        verify(producer).send(eq("latest"), captor.capture());

        BatteryDecisionSnapshot published = captor.getValue();

        assertEquals("export_to_grid", published.getTargetState());
        assertNotNull(published.getAtTime());
    }

    @Test
    public void receiveImportRateDoesNothingWhenSnapshotIsNull() {
        logic.receiveImportRate(null);

        verify(topicSlotService, never()).saveDoubleSlot(any(), any(), any());
        verify(topicSlotService, never()).saveTextSlot(any(), any(), any());
        verifyNoInteractions(batteryDecisionService);
        verifyNoInteractions(producer);
    }

    @Test
    public void receiveImportRateDoesNothingWhenRateIsMissing() {
        CurrentRateSnapshot snapshot = new CurrentRateSnapshot();

        logic.receiveImportRate(snapshot);

        verify(topicSlotService, never()).saveDoubleSlot(any(), any(), any());
        verify(topicSlotService, never()).saveTextSlot(any(), any(), any());
        verifyNoInteractions(batteryDecisionService);
        verifyNoInteractions(producer);
    }

    @Test
    public void receiveExportRateDoesNothingWhenSnapshotIsNull() {
        logic.receiveExportRate(null);

        verify(topicSlotService, never()).saveDoubleSlot(any(), any(), any());
        verify(topicSlotService, never()).saveTextSlot(any(), any(), any());
        verifyNoInteractions(batteryDecisionService);
        verifyNoInteractions(producer);
    }

    @Test
    public void receiveExportRateDoesNothingWhenRateIsMissing() {
        CurrentRateSnapshot snapshot = new CurrentRateSnapshot();

        logic.receiveExportRate(snapshot);

        verify(topicSlotService, never()).saveDoubleSlot(any(), any(), any());
        verify(topicSlotService, never()).saveTextSlot(any(), any(), any());
        verifyNoInteractions(batteryDecisionService);
        verifyNoInteractions(producer);
    }

    @Test
    public void receiveImportRateDoesNotPublishWhenExportRateMissing() {
        when(topicSlotService.getDoubleValue("current-import-rate", "rate"))
                .thenReturn(Optional.of(7.0));
        when(topicSlotService.getDoubleValue("current-export-rate", "rate"))
                .thenReturn(Optional.empty());

        CurrentRateSnapshot snapshot = new CurrentRateSnapshot();
        snapshot.setRate(7.0);

        logic.receiveImportRate(snapshot);

        verify(topicSlotService).saveDoubleSlot("current-import-rate", "rate", 7.0);
        verify(topicSlotService, never()).saveTextSlot(
                eq("battery-decision"),
                eq("targetState"),
                any()
        );
        verifyNoInteractions(batteryDecisionService);
        verifyNoInteractions(producer);
    }

    @Test
    public void receiveExportRateDoesNotPublishWhenImportRateMissing() {
        when(topicSlotService.getDoubleValue("current-import-rate", "rate"))
                .thenReturn(Optional.empty());
        when(topicSlotService.getDoubleValue("current-export-rate", "rate"))
                .thenReturn(Optional.of(18.0));

        CurrentRateSnapshot snapshot = new CurrentRateSnapshot();
        snapshot.setRate(18.0);

        logic.receiveExportRate(snapshot);

        verify(topicSlotService).saveDoubleSlot("current-export-rate", "rate", 18.0);
        verify(topicSlotService, never()).saveTextSlot(
                eq("battery-decision"),
                eq("targetState"),
                any()
        );
        verifyNoInteractions(batteryDecisionService);
        verifyNoInteractions(producer);
    }

    @Test
    public void retryUntilBatteryDecisionAvailableDoesNothingWhenDecisionAlreadyExists() {
        when(topicSlotService.getTextValue("battery-decision", "targetState"))
                .thenReturn(Optional.of("sell_excess"));

        logic.retryUntilBatteryDecisionAvailable();

        verify(topicSlotService).getTextValue("battery-decision", "targetState");
        verify(topicSlotService, never()).getDoubleValue("current-import-rate", "rate");
        verify(topicSlotService, never()).getDoubleValue("current-export-rate", "rate");
        verifyNoInteractions(batteryDecisionService);
        verifyNoInteractions(producer);
    }

    @Test
    public void retryUntilBatteryDecisionAvailablePublishesWhenInputsExist() {
        when(topicSlotService.getTextValue("battery-decision", "targetState"))
                .thenReturn(Optional.empty());
        when(topicSlotService.getDoubleValue("current-import-rate", "rate"))
                .thenReturn(Optional.of(9.0));
        when(topicSlotService.getDoubleValue("current-export-rate", "rate"))
                .thenReturn(Optional.of(16.0));
        when(batteryDecisionService.decideTargetState(9.0, 16.0))
                .thenReturn("import_to_100");

        logic.retryUntilBatteryDecisionAvailable();

        verify(topicSlotService).saveTextSlot(
                "battery-decision",
                "targetState",
                "import_to_100"
        );
        verify(topicSlotService).saveTimestampSlot(
                eq("battery-decision"),
                eq("atTime"),
                any(Instant.class)
        );
        verify(producer).send(eq("latest"), any(BatteryDecisionSnapshot.class));
    }

    @Test
    public void retryUntilBatteryDecisionAvailableDoesNothingWhenInputMissing() {
        when(topicSlotService.getTextValue("battery-decision", "targetState"))
                .thenReturn(Optional.empty());
        when(topicSlotService.getDoubleValue("current-import-rate", "rate"))
                .thenReturn(Optional.of(9.0));
        when(topicSlotService.getDoubleValue("current-export-rate", "rate"))
                .thenReturn(Optional.empty());

        logic.retryUntilBatteryDecisionAvailable();

        verify(topicSlotService, never()).saveTextSlot(
                eq("battery-decision"),
                eq("targetState"),
                any()
        );
        verifyNoInteractions(batteryDecisionService);
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
