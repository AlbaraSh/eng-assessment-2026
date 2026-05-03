package uk.ac.york.eng2.reactive.resources.importrate;

import io.micronaut.serde.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.ac.york.cs.eng2.reactive.rates.model.AgileRate;
import uk.ac.york.cs.eng2.reactive.rates.model.AgileRateResult;
import uk.ac.york.cs.eng2.reactive.rates.model.AgileRatesResponse;
import uk.ac.york.cs.eng2.reactive.rates.model.AgileSlotRates;
import uk.ac.york.eng2.reactive.components.AgileRatesSnapshot;
import uk.ac.york.eng2.reactive.components.CurrentRateSnapshot;
import uk.ac.york.eng2.reactive.components.importrate.CurrentImportRateLogic;
import uk.ac.york.eng2.reactive.kafka.CurrentImportRateProducer;
import uk.ac.york.eng2.reactive.service.ComponentService;
import uk.ac.york.eng2.reactive.service.TopicSlotService;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class CurrentImportRateLogicTest {

    private CurrentImportRateLogic logic;
    private ComponentService componentService;
    private TopicSlotService topicSlotService;
    private CurrentImportRateProducer producer;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        logic = new CurrentImportRateLogic();

        componentService = mock(ComponentService.class);
        topicSlotService = mock(TopicSlotService.class);
        producer = mock(CurrentImportRateProducer.class);
        objectMapper = mock(ObjectMapper.class);

        setField(logic, "componentService", componentService);
        setField(logic, "topicSlotService", topicSlotService);
        setField(logic, "producer", producer);
        setField(logic, "objectMapper", objectMapper);
    }

    @Test
    public void registerEnsuresComponentExists() {
        logic.register();
        verify(componentService).ensureExists("CurrentImportRateExtractor");
    }

    @Test
    public void receiveExtractsCurrentImportRateAndPublishes() throws Exception {
        Instant now = Instant.now();
        ZonedDateTime start = ZonedDateTime.ofInstant(now.minusSeconds(300), ZoneOffset.UTC);
        ZonedDateTime end = ZonedDateTime.ofInstant(now.plusSeconds(1500), ZoneOffset.UTC);

        AgileRatesResponse response = new AgileRatesResponse();
        response.setRates(List.of(slot(start, end, 7.5, 18.0)));

        when(objectMapper.readValue("{json}", AgileRatesResponse.class)).thenReturn(response);

        AgileRatesSnapshot snapshot = new AgileRatesSnapshot();
        snapshot.setJson("{json}");

        logic.receive(snapshot);

        verify(topicSlotService).saveDoubleSlot("current-import-rate", "rate", 7.5);
        verify(topicSlotService).saveTimestampSlot(eq("current-import-rate"), eq("atTime"), any(Instant.class));

        ArgumentCaptor<CurrentRateSnapshot> captor = ArgumentCaptor.forClass(CurrentRateSnapshot.class);
        verify(producer).send(eq("latest"), captor.capture());

        CurrentRateSnapshot published = captor.getValue();

        assertEquals(7.5, published.getRate());
        assertNotNull(published.getAtTime());
    }

    @Test
    public void receiveDoesNothingWhenSnapshotIsNull() {
        logic.receive(null);

        verifyNoInteractions(objectMapper);
        verify(topicSlotService, never()).saveDoubleSlot(any(), any(), any());
        verify(topicSlotService, never()).saveTimestampSlot(any(), any(), any());
        verifyNoInteractions(producer);
    }

    @Test
    public void receiveDoesNothingWhenSnapshotJsonIsMissing() {
        AgileRatesSnapshot snapshot = new AgileRatesSnapshot();

        logic.receive(snapshot);

        verifyNoInteractions(objectMapper);
        verify(topicSlotService, never()).saveDoubleSlot(any(), any(), any());
        verify(topicSlotService, never()).saveTimestampSlot(any(), any(), any());
        verifyNoInteractions(producer);
    }

    @Test
    public void receiveDoesNothingWhenSnapshotJsonIsBlank() {
        AgileRatesSnapshot snapshot = new AgileRatesSnapshot();
        snapshot.setJson("   ");

        logic.receive(snapshot);

        verifyNoInteractions(objectMapper);
        verify(topicSlotService, never()).saveDoubleSlot(any(), any(), any());
        verify(topicSlotService, never()).saveTimestampSlot(any(), any(), any());
        verifyNoInteractions(producer);
    }

    @Test
    public void receiveDoesNothingWhenNoCurrentSlot() throws Exception {
        Instant now = Instant.now();
        ZonedDateTime start = ZonedDateTime.ofInstant(now.minusSeconds(3600), ZoneOffset.UTC);
        ZonedDateTime end = ZonedDateTime.ofInstant(now.minusSeconds(1800), ZoneOffset.UTC);

        AgileRatesResponse response = new AgileRatesResponse();
        response.setRates(List.of(slot(start, end, 7.5, 18.0)));

        when(objectMapper.readValue("{json}", AgileRatesResponse.class)).thenReturn(response);

        AgileRatesSnapshot snapshot = new AgileRatesSnapshot();
        snapshot.setJson("{json}");

        logic.receive(snapshot);

        verify(topicSlotService, never()).saveDoubleSlot(any(), any(), any());
        verify(topicSlotService, never()).saveTimestampSlot(any(), any(), any());
        verifyNoInteractions(producer);
    }

    @Test
    public void receiveDoesNothingWhenImportRateIsMissing() throws Exception {
        Instant now = Instant.now();
        ZonedDateTime start = ZonedDateTime.ofInstant(now.minusSeconds(300), ZoneOffset.UTC);
        ZonedDateTime end = ZonedDateTime.ofInstant(now.plusSeconds(1500), ZoneOffset.UTC);

        AgileSlotRates slot = slot(start, end, 7.5, 18.0);
        slot.setAgileRate(null);

        AgileRatesResponse response = new AgileRatesResponse();
        response.setRates(List.of(slot));

        when(objectMapper.readValue("{json}", AgileRatesResponse.class)).thenReturn(response);

        AgileRatesSnapshot snapshot = new AgileRatesSnapshot();
        snapshot.setJson("{json}");

        logic.receive(snapshot);

        verify(topicSlotService, never()).saveDoubleSlot(any(), any(), any());
        verify(topicSlotService, never()).saveTimestampSlot(any(), any(), any());
        verifyNoInteractions(producer);
    }

    @Test
    public void recomputeEvery30MinutesUsesStoredJsonAndPublishes() throws Exception {
        Instant now = Instant.now();
        ZonedDateTime start = ZonedDateTime.ofInstant(now.minusSeconds(300), ZoneOffset.UTC);
        ZonedDateTime end = ZonedDateTime.ofInstant(now.plusSeconds(1500), ZoneOffset.UTC);

        AgileRatesResponse response = new AgileRatesResponse();
        response.setRates(List.of(slot(start, end, 6.5, 17.0)));

        when(topicSlotService.getTextValue("agile-rates", "json")).thenReturn(Optional.of("{stored-json}"));
        when(objectMapper.readValue("{stored-json}", AgileRatesResponse.class)).thenReturn(response);

        logic.recomputeEvery30Minutes();

        verify(topicSlotService).getTextValue("agile-rates", "json");
        verify(topicSlotService).saveDoubleSlot("current-import-rate", "rate", 6.5);
        verify(topicSlotService).saveTimestampSlot(eq("current-import-rate"), eq("atTime"), any(Instant.class));

        ArgumentCaptor<CurrentRateSnapshot> captor = ArgumentCaptor.forClass(CurrentRateSnapshot.class);
        verify(producer).send(eq("latest"), captor.capture());

        CurrentRateSnapshot published = captor.getValue();

        assertEquals(6.5, published.getRate());
        assertNotNull(published.getAtTime());
    }

    @Test
    public void recomputeEvery30MinutesDoesNothingWhenStoredJsonMissing() {
        when(topicSlotService.getTextValue("agile-rates", "json")).thenReturn(Optional.empty());

        logic.recomputeEvery30Minutes();

        verify(topicSlotService).getTextValue("agile-rates", "json");
        verify(topicSlotService, never()).saveDoubleSlot(any(), any(), any());
        verify(topicSlotService, never()).saveTimestampSlot(any(), any(), any());
        verifyNoInteractions(producer);
        verifyNoInteractions(objectMapper);
    }

    @Test
    public void recomputeEvery30MinutesDoesNothingWhenStoredJsonIsBlank() {
        when(topicSlotService.getTextValue("agile-rates", "json")).thenReturn(Optional.of("   "));

        logic.recomputeEvery30Minutes();

        verify(topicSlotService).getTextValue("agile-rates", "json");
        verify(topicSlotService, never()).saveDoubleSlot(any(), any(), any());
        verify(topicSlotService, never()).saveTimestampSlot(any(), any(), any());
        verifyNoInteractions(producer);
        verifyNoInteractions(objectMapper);
    }

    @Test
    public void retryUntilCurrentImportRateAvailableDoesNothingWhenRateAlreadyExists() {
        when(topicSlotService.getDoubleValue("current-import-rate", "rate")).thenReturn(Optional.of(7.5));

        logic.retryUntilCurrentImportRateAvailable();

        verify(topicSlotService).getDoubleValue("current-import-rate", "rate");
        verify(topicSlotService, never()).getTextValue("agile-rates", "json");
        verifyNoInteractions(objectMapper);
        verifyNoInteractions(producer);
    }

    @Test
    public void retryUntilCurrentImportRateAvailableDoesNothingWhenAgileRatesJsonMissing() {
        when(topicSlotService.getDoubleValue("current-import-rate", "rate")).thenReturn(Optional.empty());
        when(topicSlotService.getTextValue("agile-rates", "json")).thenReturn(Optional.empty());

        logic.retryUntilCurrentImportRateAvailable();

        verify(topicSlotService).getDoubleValue("current-import-rate", "rate");
        verify(topicSlotService).getTextValue("agile-rates", "json");
        verifyNoInteractions(objectMapper);
        verifyNoInteractions(producer);
    }

    @Test
    public void retryUntilCurrentImportRateAvailableUsesStoredJsonWhenRateMissing() throws Exception {
        Instant now = Instant.now();
        ZonedDateTime start = ZonedDateTime.ofInstant(now.minusSeconds(300), ZoneOffset.UTC);
        ZonedDateTime end = ZonedDateTime.ofInstant(now.plusSeconds(1500), ZoneOffset.UTC);

        AgileRatesResponse response = new AgileRatesResponse();
        response.setRates(List.of(slot(start, end, 5.5, 16.0)));

        when(topicSlotService.getDoubleValue("current-import-rate", "rate")).thenReturn(Optional.empty());
        when(topicSlotService.getTextValue("agile-rates", "json")).thenReturn(Optional.of("{stored-json}"));
        when(objectMapper.readValue("{stored-json}", AgileRatesResponse.class)).thenReturn(response);

        logic.retryUntilCurrentImportRateAvailable();

        verify(topicSlotService).saveDoubleSlot("current-import-rate", "rate", 5.5);
        verify(topicSlotService).saveTimestampSlot(eq("current-import-rate"), eq("atTime"), any(Instant.class));
        verify(producer).send(eq("latest"), any(CurrentRateSnapshot.class));
    }

    private AgileSlotRates slot(ZonedDateTime start, ZonedDateTime end, double importRate, double exportRate) {
        AgileRateResult importResult = new AgileRateResult();
        importResult.setRate(importRate);

        AgileRate importAgileRate = new AgileRate();
        importAgileRate.setResult(importResult);

        AgileRateResult exportResult = new AgileRateResult();
        exportResult.setRate(exportRate);

        AgileRate exportAgileRate = new AgileRate();
        exportAgileRate.setResult(exportResult);

        AgileSlotRates slot = new AgileSlotRates();
        slot.setDeliveryStart(start);
        slot.setDeliveryEnd(end);
        slot.setAgileRate(importAgileRate);
        slot.setAgileOutgoingRate(exportAgileRate);

        return slot;
    }

    private static void setField(Object target, String fieldName, Object value) {
        try {
            java.lang.reflect.Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
