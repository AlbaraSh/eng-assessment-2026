package uk.ac.york.eng2.integration;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import uk.ac.york.eng2.end2end.reactive.api.ComponentsApi;
import uk.ac.york.eng2.end2end.reactive.api.TopicSlotsApi;
import uk.ac.york.eng2.end2end.reactive.model.ComponentDTO;
import uk.ac.york.eng2.end2end.reactive.model.TopicSlotDTO;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
public class ReactiveTest {

    @Inject
    private ComponentsApi componentsApi;

    @Inject
    private TopicSlotsApi topicSlotsApi;

    @Test
    public void reactiveComponentsAppear() {
        await().atMost(Duration.ofSeconds(90)).until(componentNamesContain(
                "AgileRatesFetcher",
                "CurrentImportRateExtractor",
                "CurrentExportRateExtractor",
                "BatteryDecisionComponent",
                "BatteryActuatorControllerComponent"
        ));
    }

    @Test
    public void reactiveTopicSlotsAppear() {
        await().atMost(Duration.ofSeconds(90)).until(topicSlotExists("agile-rates", "json"));
        await().atMost(Duration.ofSeconds(90)).until(topicSlotExists("current-import-rate", "rate"));
        await().atMost(Duration.ofSeconds(90)).until(topicSlotExists("current-export-rate", "rate"));
    }

    @Test
    public void batteryDecisionAppears() {
        await().atMost(Duration.ofSeconds(90)).until(topicSlotExists("battery-decision", "targetState"));
    }

    protected Callable<Boolean> componentNamesContain(String... names) {
        return () -> {
            List<ComponentDTO> components = componentsApi.getComponents();
            for (String name : names) {
                boolean found = components.stream().anyMatch(c -> name.equals(c.getName()));
                if (!found) {
                    return false;
                }
            }
            return true;
        };
    }

    protected Callable<Boolean> topicSlotExists(String topicName, String slotName) {
        return () -> {
            List<TopicSlotDTO> topicSlots = topicSlotsApi.getTopicSlots();
            return topicSlots.stream().anyMatch(ts ->
                    topicName.equals(ts.getTopicName()) && slotName.equals(ts.getSlotName())
            );
        };
    }
}