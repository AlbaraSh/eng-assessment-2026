package uk.ac.york.eng2.reactive.kafka;

import io.micronaut.configuration.kafka.annotation.KafkaClient;
import io.micronaut.configuration.kafka.annotation.KafkaKey;
import io.micronaut.configuration.kafka.annotation.Topic;
import uk.ac.york.eng2.reactive.components.AgileRatesSnapshot;

@KafkaClient
public interface AgileRatesProducer {

    @Topic("agile-rates")
    void send(@KafkaKey String key, AgileRatesSnapshot snapshot);
}