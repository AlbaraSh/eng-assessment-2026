package uk.ac.york.eng2.reactive.resources.batteryactuator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.ac.york.eng2.reactive.components.BatteryDecisionSnapshot;
import uk.ac.york.eng2.reactive.components.batteryactuator.BatteryActuatorLogic;
import uk.ac.york.eng2.reactive.iot.api.ActuatorsApi;
import uk.ac.york.eng2.reactive.iot.model.ActuatorDTO;
import uk.ac.york.eng2.reactive.iot.model.ActuatorUpdateDTO;
import uk.ac.york.eng2.reactive.service.ComponentService;
import uk.ac.york.eng2.reactive.service.TopicSlotService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class BatteryActuatorLogicTest {

    private BatteryActuatorLogic logic;
    private ComponentService componentService;
    private TopicSlotService topicSlotService;
    private ActuatorsApi actuatorsApi;

    @BeforeEach
    public void setUp() {
        logic = new BatteryActuatorLogic();

        componentService = mock(ComponentService.class);
        topicSlotService = mock(TopicSlotService.class);
        actuatorsApi = mock(ActuatorsApi.class);

        setField(logic, "componentService", componentService);
        setField(logic, "topicSlotService", topicSlotService);
        setField(logic, "actuatorsApi", actuatorsApi);
    }

    @Test
    public void registerEnsuresComponentExists() {
        logic.register();
        verify(componentService).ensureExists("BatteryActuatorControllerComponent");
    }

    @Test
    public void receiveUpdatesOnlyBatteryActuators() {
        ActuatorDTO battery = new ActuatorDTO();
        battery.setId(1L);
        battery.setName("HomeBattery");
        battery.setType("battery");
        battery.setRoomId(10L);
        battery.setTargetState("sell_excess");

        ActuatorDTO heater = new ActuatorDTO();
        heater.setId(2L);
        heater.setName("Heater1");
        heater.setType("heater");
        heater.setRoomId(11L);
        heater.setTargetState("off");

        when(actuatorsApi.getActuators()).thenReturn(List.of(battery, heater));

        BatteryDecisionSnapshot decision = new BatteryDecisionSnapshot();
        decision.setTargetState("export_to_grid");

        logic.receive(decision);

        ArgumentCaptor<ActuatorUpdateDTO> captor = ArgumentCaptor.forClass(ActuatorUpdateDTO.class);

        verify(actuatorsApi).updateActuator(eq(1L), captor.capture());
        verify(actuatorsApi, never()).updateActuator(eq(2L), any());

        ActuatorUpdateDTO updateDTO = captor.getValue();

        assertEquals("HomeBattery", updateDTO.getName());
        assertEquals("battery", updateDTO.getType());
        assertEquals(10L, updateDTO.getRoomId());
        assertEquals("export_to_grid", updateDTO.getTargetState());
    }

    @Test
    public void receiveDoesNotUpdateBatteryWhenTargetStateAlreadyMatches() {
        ActuatorDTO battery = new ActuatorDTO();
        battery.setId(1L);
        battery.setName("HomeBattery");
        battery.setType("battery");
        battery.setRoomId(10L);
        battery.setTargetState("export_to_grid");

        when(actuatorsApi.getActuators()).thenReturn(List.of(battery));

        BatteryDecisionSnapshot decision = new BatteryDecisionSnapshot();
        decision.setTargetState("export_to_grid");

        logic.receive(decision);

        verify(actuatorsApi).getActuators();
        verify(actuatorsApi, never()).updateActuator(any(), any());
    }

    @Test
    public void receiveIgnoresNullSnapshot() {
        logic.receive(null);

        verifyNoInteractions(actuatorsApi);
    }

    @Test
    public void receiveIgnoresNullTargetState() {
        BatteryDecisionSnapshot decision = new BatteryDecisionSnapshot();

        logic.receive(decision);

        verifyNoInteractions(actuatorsApi);
    }

    @Test
    public void receiveIgnoresBlankTargetState() {
        BatteryDecisionSnapshot decision = new BatteryDecisionSnapshot();
        decision.setTargetState(" ");

        logic.receive(decision);

        verifyNoInteractions(actuatorsApi);
    }

    @Test
    public void receiveIgnoresNonBatteryActuators() {
        ActuatorDTO heater = new ActuatorDTO();
        heater.setId(2L);
        heater.setName("Heater1");
        heater.setType("heater");
        heater.setRoomId(11L);
        heater.setTargetState("off");

        when(actuatorsApi.getActuators()).thenReturn(List.of(heater));

        BatteryDecisionSnapshot decision = new BatteryDecisionSnapshot();
        decision.setTargetState("export_to_grid");

        logic.receive(decision);

        verify(actuatorsApi).getActuators();
        verify(actuatorsApi, never()).updateActuator(any(), any());
    }

    @Test
    public void receiveIgnoresBatteryActuatorWithoutId() {
        ActuatorDTO battery = new ActuatorDTO();
        battery.setName("HomeBattery");
        battery.setType("battery");
        battery.setRoomId(10L);
        battery.setTargetState("sell_excess");

        when(actuatorsApi.getActuators()).thenReturn(List.of(battery));

        BatteryDecisionSnapshot decision = new BatteryDecisionSnapshot();
        decision.setTargetState("export_to_grid");

        logic.receive(decision);

        verify(actuatorsApi).getActuators();
        verify(actuatorsApi, never()).updateActuator(any(), any());
    }

    @Test
    public void reapplyLatestDecisionIfNeededUsesStoredDecision() {
        ActuatorDTO battery = new ActuatorDTO();
        battery.setId(1L);
        battery.setName("HomeBattery");
        battery.setType("battery");
        battery.setRoomId(10L);
        battery.setTargetState("sell_excess");

        when(topicSlotService.getTextValue("battery-decision", "targetState"))
                .thenReturn(Optional.of("export_to_grid"));
        when(actuatorsApi.getActuators()).thenReturn(List.of(battery));

        logic.reapplyLatestDecisionIfNeeded();

        ArgumentCaptor<ActuatorUpdateDTO> captor = ArgumentCaptor.forClass(ActuatorUpdateDTO.class);

        verify(topicSlotService).getTextValue("battery-decision", "targetState");
        verify(actuatorsApi).updateActuator(eq(1L), captor.capture());

        ActuatorUpdateDTO updateDTO = captor.getValue();

        assertEquals("HomeBattery", updateDTO.getName());
        assertEquals("battery", updateDTO.getType());
        assertEquals(10L, updateDTO.getRoomId());
        assertEquals("export_to_grid", updateDTO.getTargetState());
    }

    @Test
    public void reapplyLatestDecisionIfNeededDoesNothingWhenStoredDecisionMissing() {
        when(topicSlotService.getTextValue("battery-decision", "targetState"))
                .thenReturn(Optional.empty());

        logic.reapplyLatestDecisionIfNeeded();

        verify(topicSlotService).getTextValue("battery-decision", "targetState");
        verifyNoInteractions(actuatorsApi);
    }

    @Test
    public void reapplyLatestDecisionIfNeededDoesNothingWhenStoredDecisionBlank() {
        when(topicSlotService.getTextValue("battery-decision", "targetState"))
                .thenReturn(Optional.of(" "));

        logic.reapplyLatestDecisionIfNeeded();

        verify(topicSlotService).getTextValue("battery-decision", "targetState");
        verifyNoInteractions(actuatorsApi);
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
