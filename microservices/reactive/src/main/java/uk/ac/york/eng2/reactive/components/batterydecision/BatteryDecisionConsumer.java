package uk.ac.york.eng2.reactive.components.batterydecision;

import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import uk.ac.york.eng2.reactive.components.CurrentRateSnapshot;

@Singleton
@KafkaListener(groupId = "battery-decision-component")
public class BatteryDecisionConsumer {

    public static final String IMPORT_RATE_TOPIC = "current-import-rate";
    public static final String EXPORT_RATE_TOPIC = "current-export-rate";

    @Inject
    private BatteryDecisionLogic logic;

    @Topic(IMPORT_RATE_TOPIC)
    public void receiveImportRate(CurrentRateSnapshot snapshot) {
        logic.receiveImportRate(snapshot);
    }

    @Topic(EXPORT_RATE_TOPIC)
    public void receiveExportRate(CurrentRateSnapshot snapshot) {
        logic.receiveExportRate(snapshot);
    }

    @Scheduled(fixedDelay = "10s", initialDelay = "5s")
    public void retryUntilBatteryDecisionAvailable() {
        logic.retryUntilBatteryDecisionAvailable();
    }
}
