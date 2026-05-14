package uk.ac.york.eng2.reactive.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.ac.york.eng2.reactive.domain.TopicSlot;
import uk.ac.york.eng2.reactive.repository.TopicSlotRepository;
import uk.ac.york.eng2.reactive.service.TopicSlotService;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TopicSlotServiceTest {

    @Mock
    private TopicSlotRepository repository;

    @InjectMocks
    private TopicSlotService service;

    @BeforeEach
    void setUp() {
        lenient().when(repository.save(any(TopicSlot.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void listAllReturnsRepositoryResults() {
        TopicSlot first = slot("agile-rates", "json");
        TopicSlot second = slot("current-import-rate", "rate");

        when(repository.findAll()).thenReturn(List.of(first, second));

        List<TopicSlot> result = service.listAll();

        assertEquals(List.of(first, second), result);
        verify(repository).findAll();
    }

    @Test
    void findByIdReturnsRepositoryResult() {
        TopicSlot slot = slot("agile-rates", "json");

        when(repository.findById(10L)).thenReturn(Optional.of(slot));

        Optional<TopicSlot> result = service.findById(10L);

        assertTrue(result.isPresent());
        assertSame(slot, result.get());
        verify(repository).findById(10L);
    }

    @Test
    void findByTopicNameReturnsRepositoryResults() {
        TopicSlot rate = slot("current-import-rate", "rate");
        TopicSlot atTime = slot("current-import-rate", "atTime");

        when(repository.findByTopicName("current-import-rate"))
                .thenReturn(List.of(rate, atTime));

        List<TopicSlot> result = service.findByTopicName("current-import-rate");

        assertEquals(2, result.size());
        assertEquals(List.of(rate, atTime), result);
        verify(repository).findByTopicName("current-import-rate");
    }

    @Test
    void findByTopicAndSlotReturnsRepositoryResult() {
        TopicSlot slot = slot("battery-decision", "targetState");

        when(repository.findByTopicNameAndSlotName("battery-decision", "targetState"))
                .thenReturn(Optional.of(slot));

        Optional<TopicSlot> result = service.findByTopicAndSlot("battery-decision", "targetState");

        assertTrue(result.isPresent());
        assertSame(slot, result.get());
        verify(repository).findByTopicNameAndSlotName("battery-decision", "targetState");
    }

    @Test
    void typedGettersReturnStoredValues() {
        Instant timestamp = Instant.parse("2026-04-08T10:30:00Z");

        TopicSlot doubleSlot = slot("current-import-rate", "rate");
        doubleSlot.setDoubleValue(7.5);

        TopicSlot longSlot = slot("example-topic", "count");
        longSlot.setLongValue(42L);

        TopicSlot textSlot = slot("battery-decision", "targetState");
        textSlot.setTextValue("sell_excess");

        TopicSlot timestampSlot = slot("current-import-rate", "atTime");
        timestampSlot.setTimestampValue(timestamp);

        when(repository.findByTopicNameAndSlotName("current-import-rate", "rate"))
                .thenReturn(Optional.of(doubleSlot));
        when(repository.findByTopicNameAndSlotName("example-topic", "count"))
                .thenReturn(Optional.of(longSlot));
        when(repository.findByTopicNameAndSlotName("battery-decision", "targetState"))
                .thenReturn(Optional.of(textSlot));
        when(repository.findByTopicNameAndSlotName("current-import-rate", "atTime"))
                .thenReturn(Optional.of(timestampSlot));

        assertEquals(Optional.of(7.5), service.getDoubleValue("current-import-rate", "rate"));
        assertEquals(Optional.of(42L), service.getLongValue("example-topic", "count"));
        assertEquals(Optional.of("sell_excess"), service.getTextValue("battery-decision", "targetState"));
        assertEquals(Optional.of(timestamp), service.getTimestampValue("current-import-rate", "atTime"));
    }

    @Test
    void typedGettersReturnEmptyWhenSlotDoesNotExist() {
        when(repository.findByTopicNameAndSlotName("missing-topic", "missing-slot"))
                .thenReturn(Optional.empty());

        assertTrue(service.getDoubleValue("missing-topic", "missing-slot").isEmpty());
        assertTrue(service.getLongValue("missing-topic", "missing-slot").isEmpty());
        assertTrue(service.getTextValue("missing-topic", "missing-slot").isEmpty());
        assertTrue(service.getTimestampValue("missing-topic", "missing-slot").isEmpty());

        verify(repository, times(4))
                .findByTopicNameAndSlotName("missing-topic", "missing-slot");
    }

    @Test
    void saveDoubleSlotCreatesNewSlot() {
        when(repository.findByTopicNameAndSlotName("current-import-rate", "rate"))
                .thenReturn(Optional.empty());

        TopicSlot result = service.saveDoubleSlot("current-import-rate", "rate", 6.25);

        assertEquals("current-import-rate", result.getTopicName());
        assertEquals("rate", result.getSlotName());
        assertEquals("double", result.getSlotType());
        assertEquals(6.25, result.getDoubleValue());
        assertNull(result.getLongValue());
        assertNull(result.getTextValue());
        assertNull(result.getTimestampValue());

        verify(repository).save(result);
    }

    @Test
    void saveLongSlotCreatesNewSlot() {
        when(repository.findByTopicNameAndSlotName("example-topic", "count"))
                .thenReturn(Optional.empty());

        TopicSlot result = service.saveLongSlot("example-topic", "count", 99L);

        assertEquals("example-topic", result.getTopicName());
        assertEquals("count", result.getSlotName());
        assertEquals("long", result.getSlotType());
        assertEquals(99L, result.getLongValue());
        assertNull(result.getDoubleValue());
        assertNull(result.getTextValue());
        assertNull(result.getTimestampValue());

        verify(repository).save(result);
    }

    @Test
    void saveTextSlotCreatesNewSlot() {
        when(repository.findByTopicNameAndSlotName("battery-decision", "targetState"))
                .thenReturn(Optional.empty());

        TopicSlot result = service.saveTextSlot("battery-decision", "targetState", "export_to_grid");

        assertEquals("battery-decision", result.getTopicName());
        assertEquals("targetState", result.getSlotName());
        assertEquals("text", result.getSlotType());
        assertEquals("export_to_grid", result.getTextValue());
        assertNull(result.getDoubleValue());
        assertNull(result.getLongValue());
        assertNull(result.getTimestampValue());

        verify(repository).save(result);
    }

    @Test
    void saveTimestampSlotCreatesNewSlot() {
        Instant value = Instant.parse("2026-04-08T10:30:00Z");

        when(repository.findByTopicNameAndSlotName("agile-rates", "validFrom"))
                .thenReturn(Optional.empty());

        TopicSlot result = service.saveTimestampSlot("agile-rates", "validFrom", value);

        assertEquals("agile-rates", result.getTopicName());
        assertEquals("validFrom", result.getSlotName());
        assertEquals("timestamp", result.getSlotType());
        assertEquals(value, result.getTimestampValue());
        assertNull(result.getDoubleValue());
        assertNull(result.getLongValue());
        assertNull(result.getTextValue());

        verify(repository).save(result);
    }

    @Test
    void saveDoubleSlotUpdatesExistingSlotAndClearsPreviousValues() {
        TopicSlot existing = slot("current-export-rate", "rate");
        existing.setSlotType("text");
        existing.setTextValue("old-value");
        existing.setLongValue(123L);
        existing.setTimestampValue(Instant.parse("2026-04-08T00:00:00Z"));

        when(repository.findByTopicNameAndSlotName("current-export-rate", "rate"))
                .thenReturn(Optional.of(existing));

        TopicSlot result = service.saveDoubleSlot("current-export-rate", "rate", 21.5);

        assertSame(existing, result);
        assertEquals("double", result.getSlotType());
        assertEquals(21.5, result.getDoubleValue());
        assertNull(result.getTextValue());
        assertNull(result.getLongValue());
        assertNull(result.getTimestampValue());

        verify(repository).save(existing);
    }

    @Test
    void saveTextSlotUpdatesExistingSlotAndClearsPreviousValues() {
        TopicSlot existing = slot("battery-decision", "targetState");
        existing.setSlotType("double");
        existing.setDoubleValue(10.0);
        existing.setLongValue(5L);
        existing.setTimestampValue(Instant.parse("2026-04-08T00:00:00Z"));

        when(repository.findByTopicNameAndSlotName("battery-decision", "targetState"))
                .thenReturn(Optional.of(existing));

        TopicSlot result = service.saveTextSlot("battery-decision", "targetState", "sell_excess");

        assertSame(existing, result);
        assertEquals("text", result.getSlotType());
        assertEquals("sell_excess", result.getTextValue());
        assertNull(result.getDoubleValue());
        assertNull(result.getLongValue());
        assertNull(result.getTimestampValue());

        verify(repository).save(existing);
    }

    @Test
    void saveTimestampSlotUpdatesExistingSlotAndClearsPreviousValues() {
        Instant newValue = Instant.parse("2026-04-08T12:00:00Z");

        TopicSlot existing = slot("agile-rates", "validTo");
        existing.setSlotType("double");
        existing.setDoubleValue(11.5);
        existing.setLongValue(3L);
        existing.setTextValue("old-text");

        when(repository.findByTopicNameAndSlotName("agile-rates", "validTo"))
                .thenReturn(Optional.of(existing));

        TopicSlot result = service.saveTimestampSlot("agile-rates", "validTo", newValue);

        assertSame(existing, result);
        assertEquals("timestamp", result.getSlotType());
        assertEquals(newValue, result.getTimestampValue());
        assertNull(result.getDoubleValue());
        assertNull(result.getLongValue());
        assertNull(result.getTextValue());

        verify(repository).save(existing);
    }

    @Test
    void saveLongSlotSavesTheExpectedObject() {
        when(repository.findByTopicNameAndSlotName("example-topic", "count"))
                .thenReturn(Optional.empty());

        service.saveLongSlot("example-topic", "count", 15L);

        ArgumentCaptor<TopicSlot> captor = ArgumentCaptor.forClass(TopicSlot.class);
        verify(repository).save(captor.capture());

        TopicSlot saved = captor.getValue();
        assertEquals("example-topic", saved.getTopicName());
        assertEquals("count", saved.getSlotName());
        assertEquals("long", saved.getSlotType());
        assertEquals(15L, saved.getLongValue());
    }

    private TopicSlot slot(String topicName, String slotName) {
        TopicSlot slot = new TopicSlot();
        slot.setTopicName(topicName);
        slot.setSlotName(slotName);
        return slot;
    }
}
