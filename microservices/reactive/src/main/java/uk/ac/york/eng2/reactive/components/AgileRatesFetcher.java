package uk.ac.york.eng2.reactive.components;

import io.micronaut.context.annotation.Context;
import io.micronaut.context.annotation.Requires;
import io.micronaut.context.env.Environment;
import io.micronaut.serde.ObjectMapper;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import uk.ac.york.eng2.reactive.kafka.AgileRatesProducer;
import uk.ac.york.eng2.reactive.service.ComponentService;
import uk.ac.york.eng2.reactive.service.TopicSlotService;
import uk.ac.york.cs.eng2.reactive.rates.api.RatesApi;
import uk.ac.york.cs.eng2.reactive.rates.model.AgileRatesResponse;
import uk.ac.york.cs.eng2.reactive.rates.model.AgileSlotRates;

import java.io.IOException;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

@Context
@Singleton
@Requires(notEnv = Environment.TEST)
public class AgileRatesFetcher {

    public static final String COMPONENT_NAME = "AgileRatesFetcher";
    public static final String TOPIC_NAME = "agile-rates";

    @Inject
    private ComponentService componentService;

    @Inject
    private TopicSlotService topicSlotService;

    @Inject
    private AgileRatesProducer producer;

    @Inject
    private RatesApi ratesApi;

    @Inject
    private ObjectMapper objectMapper;

    @PostConstruct
    public void registerAndFetch() {
        componentService.ensureExists(COMPONENT_NAME);
        tryFetch();
    }

    @Scheduled(fixedDelay = "12h", initialDelay = "12h")
    public void fetchEvery12Hours() {
        tryFetch();
    }

    @Scheduled(fixedDelay = "10s", initialDelay = "5s")
    public void retryUntilAvailable() {
        boolean alreadyFetched = topicSlotService.getTextValue(TOPIC_NAME, "json").isPresent();
        if (!alreadyFetched) {
            tryFetch();
        }
    }

    protected void tryFetch() {
        try {
            fetchAndPublish();
        } catch (Exception e) {
            System.err.println("Agile rates fetch failed: " + e.getMessage());
        }
    }

    protected void fetchAndPublish() {
        AgileRatesResponse response = ratesApi.getRates();
        AgileRatesSnapshot snapshot = toSnapshot(response);

        topicSlotService.saveTextSlot(TOPIC_NAME, "json", snapshot.getJson());

        if (snapshot.getValidFrom() != null) {
            topicSlotService.saveTimestampSlot(TOPIC_NAME, "validFrom", snapshot.getValidFrom());
        }

        if (snapshot.getValidTo() != null) {
            topicSlotService.saveTimestampSlot(TOPIC_NAME, "validTo", snapshot.getValidTo());
        }

        producer.send("latest", snapshot);
    }

    protected AgileRatesSnapshot toSnapshot(AgileRatesResponse response) {
        AgileRatesSnapshot snapshot = new AgileRatesSnapshot();
        snapshot.setJson(toJson(response));

        List<AgileSlotRates> rates = response.getRates();
        if (rates != null && !rates.isEmpty()) {
            AgileSlotRates first = rates.stream()
                    .filter(r -> r.getDeliveryStart() != null)
                    .min(Comparator.comparing(AgileSlotRates::getDeliveryStart))
                    .orElse(null);

            AgileSlotRates last = rates.stream()
                    .filter(r -> r.getDeliveryEnd() != null)
                    .max(Comparator.comparing(AgileSlotRates::getDeliveryEnd))
                    .orElse(null);

            if (first != null) {
                snapshot.setValidFrom(first.getDeliveryStart().toInstant());
            }

            if (last != null) {
                snapshot.setValidTo(last.getDeliveryEnd().toInstant());
            }
        }

        return snapshot;
    }

    protected String toJson(AgileRatesResponse response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (IOException e) {
            throw new IllegalStateException("Could not serialize agile rates response", e);
        }
    }
}