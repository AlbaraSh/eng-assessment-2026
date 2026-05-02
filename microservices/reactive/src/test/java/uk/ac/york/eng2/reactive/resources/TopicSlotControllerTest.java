package uk.ac.york.eng2.reactive.resources;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.york.eng2.reactive.domain.TopicSlot;
import uk.ac.york.eng2.reactive.dto.TopicSlotDTO;
import uk.ac.york.eng2.reactive.repository.ComponentRepository;
import uk.ac.york.eng2.reactive.repository.TopicSlotRepository;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest(transactional = false)
public class TopicSlotControllerTest {

    @Inject
    private TopicSlotClient client;

    @Inject
    private ComponentRepository componentRepository;

    @Inject
    private TopicSlotRepository topicSlotRepository;

    @BeforeEach
    public void setUp() {
        topicSlotRepository.deleteAll();
        componentRepository.deleteAll();
    }

    @Test
    public void listNoTopicSlots() {
        HttpResponse<List<TopicSlotDTO>> response = client.list();
        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals(0, response.body().size());
    }

    @Test
    public void createThenList() {
        TopicSlot slot = new TopicSlot();
        slot.setTopicName("current-import-rate");
        slot.setSlotName("rate");
        slot.setSlotType("double");
        slot.setDoubleValue(7.5);
        slot = topicSlotRepository.save(slot);

        HttpResponse<List<TopicSlotDTO>> response = client.list();
        assertEquals(HttpStatus.OK, response.getStatus());

        List<TopicSlotDTO> slots = response.body();
        assertEquals(1, slots.size());

        TopicSlotDTO dto = slots.get(0);
        assertEquals(slot.getId(), dto.getId());
        assertEquals("current-import-rate", dto.getTopicName());
        assertEquals("rate", dto.getSlotName());
        assertEquals("double", dto.getSlotType());
        assertEquals(7.5, dto.getDoubleValue());
    }

    @Test
    public void createThenGet() {
        TopicSlot slot = new TopicSlot();
        slot.setTopicName("agile-rates");
        slot.setSlotName("validFrom");
        slot.setSlotType("timestamp");
        slot.setTimestampValue(Instant.parse("2026-04-08T00:00:00Z"));
        slot = topicSlotRepository.save(slot);

        HttpResponse<TopicSlotDTO> response = client.get(slot.getId());
        assertEquals(HttpStatus.OK, response.getStatus());

        TopicSlotDTO dto = response.body();
        assertEquals(slot.getId(), dto.getId());
        assertEquals("agile-rates", dto.getTopicName());
        assertEquals("validFrom", dto.getSlotName());
        assertEquals("timestamp", dto.getSlotType());
        assertEquals(Instant.parse("2026-04-08T00:00:00Z"), dto.getTimestampValue());
    }

    @Test
    public void getMissingTopicSlotFails() {
        HttpResponse<TopicSlotDTO> response = client.get(-1L);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatus());
    }

    @Test
    public void getTopicSlotsForTopic() {
        TopicSlot importRate = new TopicSlot();
        importRate.setTopicName("current-import-rate");
        importRate.setSlotName("rate");
        importRate.setSlotType("double");
        importRate.setDoubleValue(6.5);
        topicSlotRepository.save(importRate);

        TopicSlot importTime = new TopicSlot();
        importTime.setTopicName("current-import-rate");
        importTime.setSlotName("atTime");
        importTime.setSlotType("timestamp");
        importTime.setTimestampValue(Instant.parse("2026-04-08T10:30:00Z"));
        topicSlotRepository.save(importTime);

        TopicSlot exportRate = new TopicSlot();
        exportRate.setTopicName("current-export-rate");
        exportRate.setSlotName("rate");
        exportRate.setSlotType("double");
        exportRate.setDoubleValue(18.0);
        topicSlotRepository.save(exportRate);

        HttpResponse<List<TopicSlotDTO>> response = client.getByTopic("current-import-rate");
        assertEquals(HttpStatus.OK, response.getStatus());

        List<TopicSlotDTO> slots = response.body();
        assertEquals(2, slots.size());
    }

    @Test
    public void getTopicSlotsForTopicWhenNoneExist() {
        HttpResponse<List<TopicSlotDTO>> response = client.getByTopic("missing-topic");
        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals(0, response.body().size());
    }

    @Test
    public void repositoryAndControllerSeeSameData() {
        TopicSlot slot = new TopicSlot();
        slot.setTopicName("agile-rates");
        slot.setSlotName("json");
        slot.setSlotType("text");
        slot.setTextValue("{\"rates\":[]}");
        slot = topicSlotRepository.save(slot);

        HttpResponse<TopicSlotDTO> response = client.get(slot.getId());
        assertEquals(HttpStatus.OK, response.getStatus());

        TopicSlotDTO dto = response.body();
        assertEquals(slot.getId(), dto.getId());
        assertEquals("agile-rates", dto.getTopicName());
        assertEquals("json", dto.getSlotName());
        assertEquals("text", dto.getSlotType());
        assertEquals("{\"rates\":[]}", dto.getTextValue());
    }
}