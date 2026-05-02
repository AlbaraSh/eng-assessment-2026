package uk.ac.york.eng2.reactive.kafka;

import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.Topic;
import uk.ac.york.eng2.reactive.components.CurrentRateSnapshot;

@KafkaClient
public interface CurrentImportRateProducer {

    @Topic("current-import-rate")
    void send(@KafkaKey String key, CurrentRateSnapshot snapshot);
}