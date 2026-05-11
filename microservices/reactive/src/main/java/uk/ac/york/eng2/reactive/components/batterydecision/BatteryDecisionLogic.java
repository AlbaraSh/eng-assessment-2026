package uk.ac.york.eng2.reactive.components.batterydecision;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import uk.ac.york.eng2.reactive.components.BatteryDecisionSnapshot;
import uk.ac.york.eng2.reactive.components.CurrentRateSnapshot;
import uk.ac.york.eng2.reactive.components.generated.GeneratedBatteryDecisionComponentBase;
import uk.ac.york.eng2.reactive.kafka.BatteryDecisionProducer;
import uk.ac.york.eng2.reactive.service.BatteryDecisionService;

import java.time.Instant;

@Singleton
public class BatteryDecisionLogic extends GeneratedBatteryDecisionComponentBase {

    public static final String IMPORT_RATE_TOPIC = "current-import-rate";
    public static final String EXPORT_RATE_TOPIC = "current-export-rate";
    public static final String BATTERY_DECISION_TOPIC = "battery-decision";

    public static final String RATE_SLOT = "rate";
    public static final String TARGET_STATE_SLOT = "targetState";
    public static final String AT_TIME_SLOT = "atTime";

    @Inject
    private BatteryDecisionService batteryDecisionService;

    @Inject
    private BatteryDecisionProducer producer;

    public void receiveImportRate(CurrentRateSnapshot snapshot) {
        if (snapshot == null || snapshot.getRate() == null) {
            return;
        }

        topicSlotService.saveDoubleSlot(IMPORT_RATE_TOPIC, RATE_SLOT, snapshot.getRate());

        if (snapshot.getAtTime() != null) {
            topicSlotService.saveTimestampSlot(IMPORT_RATE_TOPIC, AT_TIME_SLOT, snapshot.getAtTime());
        }

        trigger();
    }

    public void receiveExportRate(CurrentRateSnapshot snapshot) {
        if (snapshot == null || snapshot.getRate() == null) {
            return;
        }

        topicSlotService.saveDoubleSlot(EXPORT_RATE_TOPIC, RATE_SLOT, snapshot.getRate());

        if (snapshot.getAtTime() != null) {
            topicSlotService.saveTimestampSlot(EXPORT_RATE_TOPIC, AT_TIME_SLOT, snapshot.getAtTime());
        }

        trigger();
    }

    public void retryUntilBatteryDecisionAvailable() {
        boolean alreadyComputed = topicSlotService
                .getTextValue(BATTERY_DECISION_TOPIC, TARGET_STATE_SLOT)
                .isPresent();

        if (alreadyComputed) {
            return;
        }

        trigger();
    }

    @Override
    protected void activate(Double importRate, Double exportRate) {
        String targetState = batteryDecisionService.decideTargetState(importRate, exportRate);
        Instant atTime = Instant.now();

        topicSlotService.saveTextSlot(BATTERY_DECISION_TOPIC, TARGET_STATE_SLOT, targetState);
        topicSlotService.saveTimestampSlot(BATTERY_DECISION_TOPIC, AT_TIME_SLOT, atTime);

        BatteryDecisionSnapshot snapshot = new BatteryDecisionSnapshot();
        snapshot.setTargetState(targetState);
        snapshot.setAtTime(atTime);

        producer.send("latest", snapshot);
    }
}