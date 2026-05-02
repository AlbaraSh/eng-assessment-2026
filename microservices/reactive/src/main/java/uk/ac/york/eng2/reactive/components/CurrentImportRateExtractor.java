package uk.ac.york.eng2.reactive.components;

import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.micronaut.scheduling.annotation.Scheduled;
import io.micronaut.serde.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import uk.ac.york.eng2.reactive.kafka.CurrentImportRateProducer;
import uk.ac.york.eng2.reactive.service.ComponentService;
import uk.ac.york.eng2.reactive.service.TopicSlotService;
import uk.ac.york.cs.eng2.reactive.rates.model.AgileRatesResponse;
import uk.ac.york.cs.eng2.reactive.rates.model.AgileSlotRates;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

@Singleton
@KafkaListener(groupId = "current-import-rate-extractor")
public class CurrentImportRateExtractor {

    public static final String COMPONENT_NAME = "CurrentImportRateExtractor";
    public static final String INPUT_TOPIC = "agile-rates";
    public static final String OUTPUT_TOPIC = "current-import-rate";
    public static final String AGILE_RATES_TOPIC = "agile-rates";
    public static final String JSON_SLOT = "json";
    public static final String RATE_SLOT = "rate";
    public static final String AT_TIME_SLOT = "atTime";

    @Inject
    private ComponentService componentService;

    @Inject
    private TopicSlotService topicSlotService;

    @Inject
    private CurrentImportRateProducer producer;

    @Inject
    private ObjectMapper objectMapper;

    @PostConstruct
    public void register() {
        componentService.ensureExists(COMPONENT_NAME);
    }

    @Topic(INPUT_TOPIC)
    public void receive(String key, AgileRatesSnapshot snapshot) {
        if (snapshot == null || snapshot.getJson() == null || snapshot.getJson().trim().isEmpty()) {
            return;
        }

        recomputeFromJson(snapshot.getJson());
    }

    @Scheduled(fixedRate = "30m", initialDelay = "30m")
    public void recomputeEvery30Minutes() {
        Optional<String> json = getLatestAgileRatesJson();
        if (json.isEmpty()) {
            return;
        }

        recomputeFromJson(json.get());
    }

    @Scheduled(fixedDelay = "10s", initialDelay = "5s")
    public void retryUntilCurrentImportRateAvailable() {
        boolean alreadyComputed = topicSlotService.getDoubleValue(OUTPUT_TOPIC, RATE_SLOT).isPresent();
        if (alreadyComputed) {
            return;
        }

        Optional<String> json = getLatestAgileRatesJson();
        if (json.isEmpty()) {
            return;
        }

        recomputeFromJson(json.get());
    }

    protected Optional<String> getLatestAgileRatesJson() {
        Optional<String> json = topicSlotService.getTextValue(AGILE_RATES_TOPIC, JSON_SLOT);
        if (json.isEmpty() || json.get().trim().isEmpty()) {
            return Optional.empty();
        }
        return json;
    }

    protected void recomputeFromJson(String json) {
        AgileRatesResponse response = parse(json);
        Optional<AgileSlotRates> currentSlot = findCurrentSlot(response.getRates(), Instant.now());

        if (currentSlot.isEmpty()) {
            return;
        }

        AgileSlotRates slot = currentSlot.get();
        Double rate = extractImportRate(slot);
        if (rate == null) {
            return;
        }

        Instant atTime = Instant.now();

        topicSlotService.saveDoubleSlot(OUTPUT_TOPIC, RATE_SLOT, rate);
        topicSlotService.saveTimestampSlot(OUTPUT_TOPIC, AT_TIME_SLOT, atTime);

        CurrentRateSnapshot currentRate = new CurrentRateSnapshot();
        currentRate.setRate(rate);
        currentRate.setAtTime(atTime);

        producer.send("latest", currentRate);
    }

    protected AgileRatesResponse parse(String json) {
        try {
            return objectMapper.readValue(json, AgileRatesResponse.class);
        } catch (Exception e) {
            throw new IllegalStateException("Could not parse agile rates JSON", e);
        }
    }

    protected Optional<AgileSlotRates> findCurrentSlot(List<AgileSlotRates> rates, Instant now) {
        if (rates == null || rates.isEmpty()) {
            return Optional.empty();
        }

        return rates.stream()
                .filter(slot -> slot.getDeliveryStart() != null)
                .filter(slot -> slot.getDeliveryEnd() != null)
                .filter(slot -> isCurrent(slot, now))
                .findFirst();
    }

    protected boolean isCurrent(AgileSlotRates slot, Instant now) {
        ZonedDateTime start = slot.getDeliveryStart();
        ZonedDateTime end = slot.getDeliveryEnd();

        Instant startInstant = start.toInstant();
        Instant endInstant = end.toInstant();

        return !now.isBefore(startInstant) && now.isBefore(endInstant);
    }

    protected Double extractImportRate(AgileSlotRates slot) {
        if (slot.getAgileRate() == null || slot.getAgileRate().getResult() == null) {
            return null;
        }
        return slot.getAgileRate().getResult().getRate();
    }
}