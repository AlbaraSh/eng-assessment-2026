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

import java.lang.reflect.Field;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class BatteryDecisionLogicTest {

    private static final String IMPORT_TOPIC = "current-import-rate";
    private static final String EXPORT_TOPIC = "current-export-rate";
    private static final String DECISION_TOPIC = "battery-decision";

    private static final String RATE_SLOT = "rate";
    private static final String TARGET_STATE_SLOT = "targetState";
    private static final String AT_TIME_SLOT = "atTime";

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

        injectField(logic, "componentService", componentService);
        injectField(logic, "topicSlotService", topicSlotService);
        injectField(logic, "batteryDecisionService", batteryDecisionService);
        injectField(logic, "producer", producer);
    }

    @Test
    public void registerGeneratedComponentEnsuresComponentExists() {
        logic.registerGeneratedComponent();

        verify(componentService).ensureExists("BatteryDecisionComponent");
    }

    @Test
    public void receiveImportRateSavesImportRateAndPublishesDecisionWhenBothRatesExist() {
        givenBothRatesExist(7.0, 18.0);

        when(batteryDecisionService.decideTargetState(7.0, 18.0))
                .thenReturn("import_to_100");

        CurrentRateSnapshot snapshot = currentRateSnapshot(
                7.0,
                "2026-04-09T10:30:00Z"
        );

        logic.receiveImportRate(snapshot);

        verify(topicSlotService).saveDoubleSlot(IMPORT_TOPIC, RATE_SLOT, 7.0);
        verify(topicSlotService).saveTimestampSlot(
                IMPORT_TOPIC,
                AT_TIME_SLOT,
                Instant.parse("2026-04-09T10:30:00Z")
        );

        verifyDecisionWasSavedAndPublished("import_to_100");
    }

    @Test
    public void receiveExportRateSavesExportRateAndPublishesDecisionWhenBothRatesExist() {
        givenBothRatesExist(12.0, 22.0);

        when(batteryDecisionService.decideTargetState(12.0, 22.0))
                .thenReturn("export_to_grid");

        CurrentRateSnapshot snapshot = currentRateSnapshot(
                22.0,
                "2026-04-09T11:00:00Z"
        );

        logic.receiveExportRate(snapshot);

        verify(topicSlotService).saveDoubleSlot(EXPORT_TOPIC, RATE_SLOT, 22.0);
        verify(topicSlotService).saveTimestampSlot(
                EXPORT_TOPIC,
                AT_TIME_SLOT,
                Instant.parse("2026-04-09T11:00:00Z")
        );

        verifyDecisionWasSavedAndPublished("export_to_grid");
    }

    @Test
    public void receiveImportRateDoesNothingWhenSnapshotIsNull() {
        logic.receiveImportRate(null);

        verifyNoSavedRateOrDecision();
    }

    @Test
    public void receiveImportRateDoesNothingWhenRateIsMissing() {
        logic.receiveImportRate(new CurrentRateSnapshot());

        verifyNoSavedRateOrDecision();
    }

    @Test
    public void triggerDoesNothingWhenImportRateIsMissing() {
        when(topicSlotService.getDoubleValue(IMPORT_TOPIC, RATE_SLOT))
                .thenReturn(Optional.empty());
        when(topicSlotService.getDoubleValue(EXPORT_TOPIC, RATE_SLOT))
                .thenReturn(Optional.of(16.0));

        logic.trigger();

        verifyNoDecisionWasMade();
    }

    @Test
    public void triggerDoesNothingWhenExportRateIsMissing() {
        when(topicSlotService.getDoubleValue(IMPORT_TOPIC, RATE_SLOT))
                .thenReturn(Optional.of(9.0));
        when(topicSlotService.getDoubleValue(EXPORT_TOPIC, RATE_SLOT))
                .thenReturn(Optional.empty());

        logic.trigger();

        verifyNoDecisionWasMade();
    }

    @Test
    public void retryUntilBatteryDecisionAvailableDoesNothingWhenDecisionAlreadyExists() {
        when(topicSlotService.getTextValue(DECISION_TOPIC, TARGET_STATE_SLOT))
                .thenReturn(Optional.of("sell_excess"));

        logic.retryUntilBatteryDecisionAvailable();

        verify(topicSlotService).getTextValue(DECISION_TOPIC, TARGET_STATE_SLOT);
        verify(topicSlotService, never()).getDoubleValue(IMPORT_TOPIC, RATE_SLOT);
        verify(topicSlotService, never()).getDoubleValue(EXPORT_TOPIC, RATE_SLOT);

        verifyNoInteractions(batteryDecisionService);
        verifyNoInteractions(producer);
    }

    @Test
    public void retryUntilBatteryDecisionAvailablePublishesWhenNoDecisionExistsAndInputsExist() {
        when(topicSlotService.getTextValue(DECISION_TOPIC, TARGET_STATE_SLOT))
                .thenReturn(Optional.empty());

        givenBothRatesExist(9.0, 16.0);

        when(batteryDecisionService.decideTargetState(9.0, 16.0))
                .thenReturn("import_to_100");

        logic.retryUntilBatteryDecisionAvailable();

        verifyDecisionWasSavedAndPublished("import_to_100");
    }

    private void givenBothRatesExist(double importRate, double exportRate) {
        when(topicSlotService.getDoubleValue(IMPORT_TOPIC, RATE_SLOT))
                .thenReturn(Optional.of(importRate));

        when(topicSlotService.getDoubleValue(EXPORT_TOPIC, RATE_SLOT))
                .thenReturn(Optional.of(exportRate));
    }

    private CurrentRateSnapshot currentRateSnapshot(double rate, String atTime) {
        CurrentRateSnapshot snapshot = new CurrentRateSnapshot();
        snapshot.setRate(rate);

        if (atTime != null) {
            snapshot.setAtTime(Instant.parse(atTime));
        }

        return snapshot;
    }

    private void verifyDecisionWasSavedAndPublished(String expectedTargetState) {
        verify(topicSlotService).saveTextSlot(
                DECISION_TOPIC,
                TARGET_STATE_SLOT,
                expectedTargetState
        );

        verify(topicSlotService).saveTimestampSlot(
                eq(DECISION_TOPIC),
                eq(AT_TIME_SLOT),
                any(Instant.class)
        );

        ArgumentCaptor<BatteryDecisionSnapshot> captor =
                ArgumentCaptor.forClass(BatteryDecisionSnapshot.class);

        verify(producer).send(eq("latest"), captor.capture());

        BatteryDecisionSnapshot published = captor.getValue();

        assertEquals(expectedTargetState, published.getTargetState());
        assertNotNull(published.getAtTime());
    }

    private void verifyNoDecisionWasMade() {
        verify(topicSlotService, never()).saveTextSlot(
                eq(DECISION_TOPIC),
                eq(TARGET_STATE_SLOT),
                any()
        );

        verify(topicSlotService, never()).saveTimestampSlot(
                eq(DECISION_TOPIC),
                eq(AT_TIME_SLOT),
                any(Instant.class)
        );

        verifyNoInteractions(batteryDecisionService);
        verifyNoInteractions(producer);
    }

    private void verifyNoSavedRateOrDecision() {
        verify(topicSlotService, never()).saveDoubleSlot(any(), any(), any());
        verify(topicSlotService, never()).saveTimestampSlot(any(), any(), any());
        verify(topicSlotService, never()).saveTextSlot(any(), any(), any());

        verifyNoInteractions(batteryDecisionService);
        verifyNoInteractions(producer);
    }

    private static void injectField(Object target, String fieldName, Object value) {
        Class<?> currentClass = target.getClass();

        while (currentClass != null) {
            try {
                Field field = currentClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        throw new RuntimeException(new NoSuchFieldException(fieldName));
    }
}