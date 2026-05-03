package uk.ac.york.eng2.reactive.components.importrate;

import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import uk.ac.york.eng2.reactive.components.AgileRatesSnapshot;

@Singleton
@KafkaListener(groupId = "current-import-rate-extractor")
public class CurrentImportRateConsumer {

    public static final String INPUT_TOPIC = "agile-rates";

    @Inject
    private CurrentImportRateLogic logic;

    @Topic(INPUT_TOPIC)
    public void receive(AgileRatesSnapshot snapshot) {
        logic.receive(snapshot);
    }

    @Scheduled(fixedRate = "30m", initialDelay = "30m")
    public void recomputeEvery30Minutes() {
        logic.recomputeEvery30Minutes();
    }

    @Scheduled(fixedDelay = "10s", initialDelay = "5s")
    public void retryUntilCurrentImportRateAvailable() {
        logic.retryUntilCurrentImportRateAvailable();
    }
}
