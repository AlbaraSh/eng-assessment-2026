package uk.ac.york.eng2.reactive.resources;

import io.micronaut.serde.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.ac.york.eng2.reactive.components.AgileRatesSnapshot;
import uk.ac.york.eng2.reactive.components.CurrentExportRateExtractor;
import uk.ac.york.eng2.reactive.components.CurrentRateSnapshot;
import uk.ac.york.eng2.reactive.kafka.CurrentExportRateProducer;
import uk.ac.york.eng2.reactive.service.ComponentService;
import uk.ac.york.eng2.reactive.service.TopicSlotService;
import uk.ac.york.cs.eng2.reactive.rates.model.AgileRate;
import uk.ac.york.cs.eng2.reactive.rates.model.AgileRateResult;
import uk.ac.york.cs.eng2.reactive.rates.model.AgileRatesResponse;
import uk.ac.york.cs.eng2.reactive.rates.model.AgileSlotRates;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class CurrentExportRateExtractorTest {

    private CurrentExportRateExtractor extractor;
    private ComponentService componentService;
    private TopicSlotService topicSlotService;
    private CurrentExportRateProducer producer;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        extractor = new CurrentExportRateExtractor();
        componentService = mock(ComponentService.class);
        topicSlotService = mock(TopicSlotService.class);
        producer = mock(CurrentExportRateProducer.class);
        objectMapper = mock(ObjectMapper.class);

        setField(extractor, "componentService", componentService);
        setField(extractor, "topicSlotService", topicSlotService);
        setField(extractor, "producer", producer);
        setField(extractor, "objectMapper", objectMapper);
    }

    @Test
    public void registerEnsuresComponentExists() {
        extractor.register();
        verify(componentService).ensureExists("CurrentExportRateExtractor");
    }

    @Test
    public void receiveExtractsCurrentExportRateAndPublishes() throws Exception {
        Instant now = Instant.now();
        ZonedDateTime start = ZonedDateTime.ofInstant(now.minusSeconds(300), ZoneOffset.UTC);
        ZonedDateTime end = ZonedDateTime.ofInstant(now.plusSeconds(1500), ZoneOffset.UTC);

        AgileRatesResponse response = new AgileRatesResponse();
        response.setRates(List.of(slot(start, end, 7.5, 18.0)));

        when(objectMapper.readValue("{json}", AgileRatesResponse.class)).thenReturn(response);

        AgileRatesSnapshot snapshot = new AgileRatesSnapshot();
        snapshot.setJson("{json}");

        extractor.receive("latest", snapshot);

        verify(topicSlotService).saveDoubleSlot("current-export-rate", "rate", 18.0);
        verify(topicSlotService).saveTimestampSlot(eq("current-export-rate"), eq("atTime"), any(Instant.class));

        ArgumentCaptor<CurrentRateSnapshot> captor = ArgumentCaptor.forClass(CurrentRateSnapshot.class);
        verify(producer).send(eq("latest"), captor.capture());

        CurrentRateSnapshot published = captor.getValue();
        assertEquals(18.0, published.getRate());
        assertNotNull(published.getAtTime());
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

        extractor.receive("latest", snapshot);

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

        extractor.recomputeEvery30Minutes();

        verify(topicSlotService).getTextValue("agile-rates", "json");
        verify(topicSlotService).saveDoubleSlot("current-export-rate", "rate", 17.0);
        verify(topicSlotService).saveTimestampSlot(eq("current-export-rate"), eq("atTime"), any(Instant.class));

        ArgumentCaptor<CurrentRateSnapshot> captor = ArgumentCaptor.forClass(CurrentRateSnapshot.class);
        verify(producer).send(eq("latest"), captor.capture());

        CurrentRateSnapshot published = captor.getValue();
        assertEquals(17.0, published.getRate());
        assertNotNull(published.getAtTime());
    }

    @Test
    public void recomputeEvery30MinutesDoesNothingWhenStoredJsonMissing() {
        when(topicSlotService.getTextValue("agile-rates", "json")).thenReturn(Optional.empty());

        extractor.recomputeEvery30Minutes();

        verify(topicSlotService).getTextValue("agile-rates", "json");
        verify(topicSlotService, never()).saveDoubleSlot(any(), any(), any());
        verify(topicSlotService, never()).saveTimestampSlot(any(), any(), any());
        verifyNoInteractions(producer);
        verifyNoInteractions(objectMapper);
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
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}