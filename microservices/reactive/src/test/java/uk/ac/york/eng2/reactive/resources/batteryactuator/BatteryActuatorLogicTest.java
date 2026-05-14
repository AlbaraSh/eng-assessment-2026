package uk.ac.york.eng2.reactive.resources.batteryactuator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.ac.york.eng2.reactive.components.BatteryDecisionSnapshot;
import uk.ac.york.eng2.reactive.components.batteryactuator.BatteryActuatorLogic;
import uk.ac.york.cs.eng2.reactive.iot.api.ActuatorsApi;
import uk.ac.york.cs.eng2.reactive.iot.model.ActuatorDTO;
import uk.ac.york.cs.eng2.reactive.iot.model.ActuatorUpdateDTO;
import uk.ac.york.eng2.reactive.service.ComponentService;
import uk.ac.york.eng2.reactive.service.TopicSlotService;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class BatteryActuatorLogicTest {

    private static final String DECISION_TOPIC = "battery-decision";
    private static final String TARGET_STATE_SLOT = "targetState";

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

        injectField(logic, "componentService", componentService);
        injectField(logic, "topicSlotService", topicSlotService);
        injectField(logic, "actuatorsApi", actuatorsApi);
    }

    @Test
    public void registerGeneratedComponentEnsuresComponentExists() {
        logic.registerGeneratedComponent();

        verify(componentService).ensureExists("BatteryActuatorControllerComponent");
    }

    @Test
    public void receiveSavesDecisionAndUpdatesOnlyBatteryActuators() {
        ActuatorDTO battery = batteryActuator("sell_excess");
        ActuatorDTO heater = heaterActuator();

        when(topicSlotService.getTextValue(DECISION_TOPIC, TARGET_STATE_SLOT))
                .thenReturn(Optional.of("export_to_grid"));
        when(actuatorsApi.getActuators())
                .thenReturn(List.of(battery, heater));

        BatteryDecisionSnapshot decision = decisionSnapshot("export_to_grid");

        logic.receive(decision);

        verify(topicSlotService).saveTextSlot(
                DECISION_TOPIC,
                TARGET_STATE_SLOT,
                "export_to_grid"
        );

        ArgumentCaptor<ActuatorUpdateDTO> captor =
                ArgumentCaptor.forClass(ActuatorUpdateDTO.class);

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
        ActuatorDTO battery = batteryActuator("export_to_grid");

        when(topicSlotService.getTextValue(DECISION_TOPIC, TARGET_STATE_SLOT))
                .thenReturn(Optional.of("export_to_grid"));
        when(actuatorsApi.getActuators())
                .thenReturn(List.of(battery));

        BatteryDecisionSnapshot decision = decisionSnapshot("export_to_grid");

        logic.receive(decision);

        verify(topicSlotService).saveTextSlot(
                DECISION_TOPIC,
                TARGET_STATE_SLOT,
                "export_to_grid"
        );
        verify(actuatorsApi).getActuators();
        verify(actuatorsApi, never()).updateActuator(any(), any());
    }

    @Test
    public void receiveIgnoresNullSnapshot() {
        logic.receive(null);

        verify(topicSlotService, never()).saveTextSlot(any(), any(), any());
        verifyNoInteractions(actuatorsApi);
    }

    @Test
    public void receiveIgnoresBlankTargetState() {
        BatteryDecisionSnapshot decision = decisionSnapshot(" ");

        logic.receive(decision);

        verify(topicSlotService, never()).saveTextSlot(any(), any(), any());
        verifyNoInteractions(actuatorsApi);
    }

    @Test
    public void reapplyLatestDecisionIfNeededUsesStoredDecision() {
        ActuatorDTO battery = batteryActuator("sell_excess");

        when(topicSlotService.getTextValue(DECISION_TOPIC, TARGET_STATE_SLOT))
                .thenReturn(Optional.of("export_to_grid"));
        when(actuatorsApi.getActuators())
                .thenReturn(List.of(battery));

        logic.reapplyLatestDecisionIfNeeded();

        ArgumentCaptor<ActuatorUpdateDTO> captor =
                ArgumentCaptor.forClass(ActuatorUpdateDTO.class);

        verify(topicSlotService).getTextValue(DECISION_TOPIC, TARGET_STATE_SLOT);
        verify(actuatorsApi).updateActuator(eq(1L), captor.capture());

        ActuatorUpdateDTO updateDTO = captor.getValue();

        assertEquals("HomeBattery", updateDTO.getName());
        assertEquals("battery", updateDTO.getType());
        assertEquals(10L, updateDTO.getRoomId());
        assertEquals("export_to_grid", updateDTO.getTargetState());
    }

    @Test
    public void reapplyLatestDecisionIfNeededDoesNothingWhenStoredDecisionMissing() {
        when(topicSlotService.getTextValue(DECISION_TOPIC, TARGET_STATE_SLOT))
                .thenReturn(Optional.empty());

        logic.reapplyLatestDecisionIfNeeded();

        verify(topicSlotService).getTextValue(DECISION_TOPIC, TARGET_STATE_SLOT);
        verifyNoInteractions(actuatorsApi);
    }

    @Test
    public void reapplyLatestDecisionIfNeededDoesNothingWhenStoredDecisionBlank() {
        when(topicSlotService.getTextValue(DECISION_TOPIC, TARGET_STATE_SLOT))
                .thenReturn(Optional.of(" "));

        logic.reapplyLatestDecisionIfNeeded();

        verify(topicSlotService).getTextValue(DECISION_TOPIC, TARGET_STATE_SLOT);
        verify(actuatorsApi, never()).updateActuator(any(), any());
    }

    private BatteryDecisionSnapshot decisionSnapshot(String targetState) {
        BatteryDecisionSnapshot decision = new BatteryDecisionSnapshot();
        decision.setTargetState(targetState);
        return decision;
    }

    private ActuatorDTO batteryActuator(String currentTargetState) {
        ActuatorDTO battery = new ActuatorDTO();
        battery.setId(1L);
        battery.setName("HomeBattery");
        battery.setType("battery");
        battery.setRoomId(10L);
        battery.setTargetState(currentTargetState);
        return battery;
    }

    private ActuatorDTO heaterActuator() {
        ActuatorDTO heater = new ActuatorDTO();
        heater.setId(2L);
        heater.setName("Heater1");
        heater.setType("heater");
        heater.setRoomId(11L);
        heater.setTargetState("off");
        return heater;
    }

    private static void injectField(Object target, String fieldName, Object value) {
        Class<?> currentClass = target.getClass();

        while (currentClass != null) {
            try {
                Field field = currentClass.getDeclaredField(fieldName);
                field.setAccessible(true);
                field.set(target, value);
                return;
            } catch (NoSuchFieldException e) {
                currentClass = currentClass.getSuperclass();
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        throw new RuntimeException(new NoSuchFieldException(fieldName));
    }
}