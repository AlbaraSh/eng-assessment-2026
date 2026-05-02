package uk.ac.york.eng2.reactive.components;

import io.micronaut.configuration.kafka.annotation.KafkaListener;
import io.micronaut.configuration.kafka.annotation.Topic;
import io.micronaut.scheduling.annotation.Scheduled;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import uk.ac.york.eng2.reactive.iot.api.ActuatorsApi;
import uk.ac.york.eng2.reactive.iot.model.ActuatorDTO;
import uk.ac.york.eng2.reactive.iot.model.ActuatorUpdateDTO;
import uk.ac.york.eng2.reactive.service.ComponentService;
import uk.ac.york.eng2.reactive.service.TopicSlotService;

import java.util.List;
import java.util.Optional;

@Singleton
@KafkaListener(groupId = "battery-actuator-controller-component")
public class BatteryActuatorControllerComponent {

    public static final String COMPONENT_NAME = "BatteryActuatorControllerComponent";
    public static final String INPUT_TOPIC = "battery-decision";
    public static final String TARGET_STATE_SLOT = "targetState";

    @Inject
    private ComponentService componentService;

    @Inject
    private TopicSlotService topicSlotService;

    @Inject
    private ActuatorsApi actuatorsApi;

    @PostConstruct
    public void register() {
        componentService.ensureExists(COMPONENT_NAME);
    }

    @Topic(INPUT_TOPIC)
    public void receive(String key, BatteryDecisionSnapshot snapshot) {
        if (snapshot == null || snapshot.getTargetState() == null || snapshot.getTargetState().trim().isEmpty()) {
            return;
        }

        applyTargetStateToBatteryActuators(snapshot.getTargetState());
    }

    @Scheduled(fixedDelay = "10s", initialDelay = "5s")
    public void reapplyLatestDecisionIfNeeded() {
        Optional<String> targetState = topicSlotService.getTextValue(INPUT_TOPIC, TARGET_STATE_SLOT);
        if (targetState.isEmpty() || targetState.get().trim().isEmpty()) {
            return;
        }

        applyTargetStateToBatteryActuators(targetState.get());
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
}