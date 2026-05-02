package uk.ac.york.eng2.reactive.resources;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import uk.ac.york.eng2.reactive.components.BatteryActuatorControllerComponent;
import uk.ac.york.eng2.reactive.components.BatteryDecisionSnapshot;
import uk.ac.york.eng2.reactive.iot.api.ActuatorsApi;
import uk.ac.york.eng2.reactive.iot.model.ActuatorDTO;
import uk.ac.york.eng2.reactive.iot.model.ActuatorUpdateDTO;
import uk.ac.york.eng2.reactive.service.ComponentService;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class BatteryActuatorControllerComponentTest {

    private BatteryActuatorControllerComponent component;
    private ComponentService componentService;
    private ActuatorsApi actuatorsApi;

    @BeforeEach
    public void setUp() {
        component = new BatteryActuatorControllerComponent();
        componentService = mock(ComponentService.class);
        actuatorsApi = mock(ActuatorsApi.class);

        setField(component, "componentService", componentService);
        setField(component, "actuatorsApi", actuatorsApi);
    }

    @Test
    public void registerEnsuresComponentExists() {
        component.register();
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

        component.receive("latest", decision);

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
    public void receiveIgnoresBlankTargetState() {
        BatteryDecisionSnapshot decision = new BatteryDecisionSnapshot();
        decision.setTargetState(" ");

        component.receive("latest", decision);

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