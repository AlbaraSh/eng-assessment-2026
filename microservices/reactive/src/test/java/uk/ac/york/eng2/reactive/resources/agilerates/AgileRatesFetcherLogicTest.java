package uk.ac.york.eng2.reactive.resources.agilerates;

import io.micronaut.serde.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.ac.york.cs.eng2.reactive.rates.model.AgileRate;
import uk.ac.york.cs.eng2.reactive.rates.model.AgileRateResult;
import uk.ac.york.cs.eng2.reactive.rates.model.AgileRatesResponse;
import uk.ac.york.cs.eng2.reactive.rates.model.AgileSlotRates;
import uk.ac.york.eng2.reactive.components.AgileRatesSnapshot;
import uk.ac.york.eng2.reactive.components.agilerates.AgileRatesFetcherLogic;
import uk.ac.york.eng2.reactive.components.agilerates.AgileRatesGateway;
import uk.ac.york.eng2.reactive.kafka.AgileRatesProducer;
import uk.ac.york.eng2.reactive.service.ComponentService;
import uk.ac.york.eng2.reactive.service.TopicSlotService;

import java.lang.reflect.Field;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class AgileRatesFetcherLogicTest {

    private AgileRatesFetcherLogic logic;
    private ComponentService componentService;
    private TopicSlotService topicSlotService;
    private AgileRatesProducer producer;
    private AgileRatesGateway agileRatesGateway;
    private ObjectMapper objectMapper;

    @BeforeEach
    public void setUp() {
        logic = new AgileRatesFetcherLogic();

        componentService = mock(ComponentService.class);
        topicSlotService = mock(TopicSlotService.class);
        producer = mock(AgileRatesProducer.class);
        agileRatesGateway = mock(AgileRatesGateway.class);
        objectMapper = mock(ObjectMapper.class);

        setField(logic, "componentService", componentService);
        setField(logic, "topicSlotService", topicSlotService);
        setField(logic, "producer", producer);
        setField(logic, "agileRatesGateway", agileRatesGateway);
        setField(logic, "objectMapper", objectMapper);
    }

    @Test
    public void registerEnsuresComponentExists() {
        logic.register();

        verify(componentService).ensureExists("AgileRatesFetcher");
    }

    @Test
    public void tryFetchSavesSlotsAndPublishes() throws Exception {
        AgileRatesResponse response = new AgileRatesResponse();
        response.setRates(List.of(
                slot("2026-04-09T11:00:00Z", "2026-04-09T11:30:00Z", 8.0, 15.0),
                slot("2026-04-09T10:30:00Z", "2026-04-09T11:00:00Z", 7.0, 14.0)
        ));

        when(agileRatesGateway.getRates()).thenReturn(response);
        when(objectMapper.writeValueAsString(response)).thenReturn("{\"rates\":[]}");

        logic.tryFetch();

        verify(topicSlotService).saveTextSlot("agile-rates", "json", "{\"rates\":[]}");

        verify(topicSlotService).saveTimestampSlot(
                "agile-rates",
                "validFrom",
                ZonedDateTime.parse("2026-04-09T10:30:00Z").toInstant()
        );

        verify(topicSlotService).saveTimestampSlot(
                "agile-rates",
                "validTo",
                ZonedDateTime.parse("2026-04-09T11:30:00Z").toInstant()
        );

        ArgumentCaptor<AgileRatesSnapshot> captor = ArgumentCaptor.forClass(AgileRatesSnapshot.class);
        verify(producer).send(eq("latest"), captor.capture());

        AgileRatesSnapshot snapshot = captor.getValue();

        assertEquals("{\"rates\":[]}", snapshot.getJson());
        assertEquals(
                ZonedDateTime.parse("2026-04-09T10:30:00Z").toInstant(),
                snapshot.getValidFrom()
        );
        assertEquals(
                ZonedDateTime.parse("2026-04-09T11:30:00Z").toInstant(),
                snapshot.getValidTo()
        );
    }

    @Test
    public void retryUntilAvailableFetchesWhenJsonSlotIsMissing() throws Exception {
        AgileRatesResponse response = new AgileRatesResponse();
        response.setRates(List.of(
                slot("2026-04-09T10:30:00Z", "2026-04-09T11:00:00Z", 7.0, 14.0)
        ));

        when(topicSlotService.getTextValue("agile-rates", "json")).thenReturn(Optional.empty());
        when(agileRatesGateway.getRates()).thenReturn(response);
        when(objectMapper.writeValueAsString(response)).thenReturn("{\"rates\":[]}");

        logic.retryUntilAvailable();

        verify(agileRatesGateway).getRates();
        verify(producer).send(eq("latest"), any(AgileRatesSnapshot.class));
    }

    @Test
    public void retryUntilAvailableDoesNothingWhenJsonSlotAlreadyExists() {
        when(topicSlotService.getTextValue("agile-rates", "json")).thenReturn(Optional.of("{\"rates\":[]}"));

        logic.retryUntilAvailable();

        verifyNoInteractions(agileRatesGateway);
        verifyNoInteractions(producer);
    }

    @Test
    public void tryFetchDoesNotThrowWhenGatewayFails() {
        when(agileRatesGateway.getRates()).thenThrow(new RuntimeException("API unavailable"));
        logic.tryFetch();
        verifyNoInteractions(producer);
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
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}