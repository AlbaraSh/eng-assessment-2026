package uk.ac.york.eng2.reactive.kafka;

import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.Topic;
import uk.ac.york.eng2.reactive.components.CurrentRateSnapshot;

@KafkaClient
public interface CurrentExportRateProducer {

    @Topic("current-export-rate")
    void send(@KafkaKey String key, CurrentRateSnapshot snapshot);
}