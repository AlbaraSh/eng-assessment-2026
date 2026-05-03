package uk.ac.york.eng2.reactive.components.batteryactuator;

import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import uk.ac.york.eng2.reactive.components.BatteryDecisionSnapshot;

@Singleton
@KafkaListener(groupId = "battery-actuator-controller-component")
public class BatteryActuatorConsumer {

    public static final String INPUT_TOPIC = "battery-decision";

    @Inject
    private BatteryActuatorLogic logic;

    @Topic(INPUT_TOPIC)
    public void receive(BatteryDecisionSnapshot snapshot) {
        logic.receive(snapshot);
    }

    @Scheduled(fixedDelay = "10s", initialDelay = "5s")
    public void reapplyLatestDecisionIfNeeded() {
        logic.reapplyLatestDecisionIfNeeded();
    }
}
