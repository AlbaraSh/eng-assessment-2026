package uk.ac.york.eng2.reactive.components.batteryactuator;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import uk.ac.york.eng2.reactive.components.BatteryDecisionSnapshot;
import uk.ac.york.eng2.reactive.components.generated.GeneratedBatteryActuatorControllerComponentBase;
import uk.ac.york.eng2.reactive.iot.api.ActuatorsApi;
import uk.ac.york.eng2.reactive.iot.model.ActuatorDTO;
import uk.ac.york.eng2.reactive.iot.model.ActuatorUpdateDTO;

import java.util.List;

@Singleton
public class BatteryActuatorLogic extends GeneratedBatteryActuatorControllerComponentBase {

    public static final String INPUT_TOPIC = "battery-decision";
    public static final String TARGET_STATE_SLOT = "targetState";

    @Inject
    private ActuatorsApi actuatorsApi;

    public void receive(BatteryDecisionSnapshot snapshot) {
        if (snapshot == null || isBlank(snapshot.getTargetState())) {
            return;
        }

        topicSlotService.saveTextSlot(INPUT_TOPIC, TARGET_STATE_SLOT, snapshot.getTargetState());
        trigger();
    }

    public void reapplyLatestDecisionIfNeeded() {
        trigger();
    }

    @Override
    protected void activate(String targetState) {
        if (isBlank(targetState)) {
            return;
        }

        applyTargetStateToBatteryActuators(targetState);
    }

    protected void applyTargetStateToBatteryActuators(String targetState) {
        List<ActuatorDTO> actuators = actuatorsApi.getActuators();

        for (ActuatorDTO actuator : actuators) {
            if (isBatteryActuator(actuator) && needsUpdate(actuator, targetState)) {
                ActuatorUpdateDTO updateDTO = new ActuatorUpdateDTO();
                updateDTO.setName(actuator.getName());
                updateDTO.setType(actuator.getType());
                updateDTO.setRoomId(actuator.getRoomId());
                updateDTO.setTargetState(targetState);

                actuatorsApi.updateActuator(actuator.getId(), updateDTO);
            }
        }
    }

    protected boolean isBatteryActuator(ActuatorDTO actuator) {
        return actuator != null
                && actuator.getType() != null
                && actuator.getId() != null
                && "battery".equalsIgnoreCase(actuator.getType());
    }

    protected boolean needsUpdate(ActuatorDTO actuator, String targetState) {
        return actuator.getTargetState() == null || !actuator.getTargetState().equals(targetState);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}