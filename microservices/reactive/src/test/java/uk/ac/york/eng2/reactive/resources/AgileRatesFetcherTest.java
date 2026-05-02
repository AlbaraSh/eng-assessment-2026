package uk.ac.york.eng2.reactive.resources;

import io.micronaut.serde.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.ac.york.eng2.reactive.components.AgileRatesFetcher;
import uk.ac.york.eng2.reactive.components.AgileRatesSnapshot;
import uk.ac.york.eng2.reactive.kafka.AgileRatesProducer;
import uk.ac.york.eng2.reactive.service.ComponentService;
import uk.ac.york.eng2.reactive.service.TopicSlotService;
import uk.ac.york.cs.eng2.reactive.rates.api.RatesApi;
import uk.ac.york.cs.eng2.reactive.rates.model.AgileRate;
import uk.ac.york.cs.eng2.reactive.rates.model.AgileRateResult;
import uk.ac.york.cs.eng2.reactive.rates.model.AgileRatesResponse;
import uk.ac.york.cs.eng2.reactive.rates.model.AgileSlotRates;

import java.time.ZonedDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class AgileRatesFetcherTest {

    private AgileRatesFetcher fetcher;
    private ComponentService componentService;
    private TopicSlotService topicSlotService;
    private AgileRatesProducer producer;
    private RatesApi ratesApi;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        fetcher = new AgileRatesFetcher();
        componentService = mock(ComponentService.class);
        topicSlotService = mock(TopicSlotService.class);
        producer = mock(AgileRatesProducer.class);
        ratesApi = mock(RatesApi.class);
        objectMapper = mock(ObjectMapper.class);

        setField(fetcher, "componentService", componentService);
        setField(fetcher, "topicSlotService", topicSlotService);
        setField(fetcher, "producer", producer);
        setField(fetcher, "ratesApi", ratesApi);
        setField(fetcher, "objectMapper", objectMapper);
    }

    @Test
    public void registerAndFetchSavesSlotsAndPublishes() throws Exception {
        AgileRatesResponse response = new AgileRatesResponse();
        response.setRates(List.of(
                slot("2026-04-09T11:00:00Z", "2026-04-09T11:30:00Z", 8.0, 15.0),
                slot("2026-04-09T10:30:00Z", "2026-04-09T11:00:00Z", 7.0, 14.0)
        ));

        when(ratesApi.getRates()).thenReturn(response);
        when(objectMapper.writeValueAsString(response)).thenReturn("{\"rates\":[]}");

        fetcher.registerAndFetch();

        verify(componentService).ensureExists("AgileRatesFetcher");
        verify(topicSlotService).saveTextSlot("agile-rates", "json", "{\"rates\":[]}");
        verify(topicSlotService).saveTimestampSlot("agile-rates", "validFrom",
                ZonedDateTime.parse("2026-04-09T10:30:00Z").toInstant());
        verify(topicSlotService).saveTimestampSlot("agile-rates", "validTo",
                ZonedDateTime.parse("2026-04-09T11:30:00Z").toInstant());

        ArgumentCaptor<AgileRatesSnapshot> captor = ArgumentCaptor.forClass(AgileRatesSnapshot.class);
        verify(producer).send(eq("latest"), captor.capture());

        AgileRatesSnapshot snapshot = captor.getValue();
        assertEquals("{\"rates\":[]}", snapshot.getJson());
        assertEquals(ZonedDateTime.parse("2026-04-09T10:30:00Z").toInstant(), snapshot.getValidFrom());
        assertEquals(ZonedDateTime.parse("2026-04-09T11:30:00Z").toInstant(), snapshot.getValidTo());
    }

    private AgileSlotRates slot(String start, String end, double importRate, double exportRate) {
        AgileRateResult importResult = new AgileRateResult();
        importResult.setRate(importRate);
        AgileRate importAgileRate = new AgileRate();
        importAgileRate.setResult(importResult);

        AgileRateResult exportResult = new AgileRateResult();
        exportResult.setRate(exportRate);
        AgileRate exportAgileRate = new AgileRate();
        exportAgileRate.setResult(exportResult);

        AgileSlotRates slot = new AgileSlotRates();
        slot.setDeliveryStart(ZonedDateTime.parse(start));
        slot.setDeliveryEnd(ZonedDateTime.parse(end));
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