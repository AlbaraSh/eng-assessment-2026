package uk.ac.york.eng2.reactive.components;

import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import uk.ac.york.eng2.reactive.kafka.BatteryDecisionProducer;
import uk.ac.york.eng2.reactive.service.BatteryDecisionService;
import uk.ac.york.eng2.reactive.service.ComponentService;
import uk.ac.york.eng2.reactive.service.TopicSlotService;

import java.time.Instant;
import java.util.Optional;

@Singleton
@KafkaListener(groupId = "battery-decision-component")
public class BatteryDecisionComponent {

    public static final String COMPONENT_NAME = "BatteryDecisionComponent";
    public static final String IMPORT_TOPIC = "current-import-rate";
    public static final String EXPORT_TOPIC = "current-export-rate";
    public static final String OUTPUT_TOPIC = "battery-decision";

    public static final String RATE_SLOT = "rate";
    public static final String TARGET_STATE_SLOT = "targetState";
    public static final String AT_TIME_SLOT = "atTime";

    @Inject
    private ComponentService componentService;

    @Inject
    private TopicSlotService topicSlotService;

    @Inject
    private BatteryDecisionService batteryDecisionService;

    @Inject
    private BatteryDecisionProducer producer;

    @PostConstruct
    public void register() {
        componentService.ensureExists(COMPONENT_NAME);
    }

    @Topic(IMPORT_TOPIC)
    public void receiveImport(String key, CurrentRateSnapshot snapshot) {
        decideAndPublish();
    }

    @Topic(EXPORT_TOPIC)
    public void receiveExport(String key, CurrentRateSnapshot snapshot) {
        decideAndPublish();
    }

    @Scheduled(fixedDelay = "10s", initialDelay = "5s")
    public void retryUntilBatteryDecisionAvailable() {
        boolean alreadyComputed = topicSlotService.getTextValue(OUTPUT_TOPIC, TARGET_STATE_SLOT).isPresent();
        if (alreadyComputed) {
            return;
        }

        decideAndPublish();
    }

    protected void decideAndPublish() {
        Optional<Double> importRate = topicSlotService.getDoubleValue(IMPORT_TOPIC, RATE_SLOT);
        Optional<Double> exportRate = topicSlotService.getDoubleValue(EXPORT_TOPIC, RATE_SLOT);

        if (importRate.isEmpty() || exportRate.isEmpty()) {
            return;
        }

        String targetState = batteryDecisionService.decideTargetState(importRate.get(), exportRate.get());
        Instant now = Instant.now();

        topicSlotService.saveTextSlot(OUTPUT_TOPIC, TARGET_STATE_SLOT, targetState);
        topicSlotService.saveTimestampSlot(OUTPUT_TOPIC, AT_TIME_SLOT, now);

        BatteryDecisionSnapshot decision = new BatteryDecisionSnapshot();
        decision.setTargetState(targetState);
        decision.setAtTime(now);

        producer.send("latest", decision);
    }
}