package uk.ac.york.eng2.integration;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;
import uk.ac.york.eng2.end2end.iot.api.ActuatorsApi;
import uk.ac.york.eng2.end2end.iot.api.RoomsApi;
import uk.ac.york.eng2.end2end.iot.model.ActuatorCreateDTO;
import uk.ac.york.eng2.end2end.iot.model.ActuatorDTO;
import uk.ac.york.eng2.end2end.iot.model.RoomCreateDTO;
import uk.ac.york.eng2.end2end.iot.model.RoomDTO;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Callable;

import static org.awaitility.Awaitility.await;

@MicronautTest
public class BatteryWorkflowTest {

    @Inject
    private RoomsApi roomsApi;

    @Inject
    private ActuatorsApi actuatorsApi;

    @Test
    public void createBatteryActuatorAndWaitForDecision() {
        String suffix = String.valueOf(System.currentTimeMillis());

        RoomCreateDTO roomDto = new RoomCreateDTO();
        roomDto.setName("UtilityRoom-" + suffix);
        roomsApi.createRoom(roomDto);

        await().atMost(Duration.ofSeconds(30)).until(roomExists(roomDto.getName()));
        long roomId = getRoomIdByName(roomDto.getName());

        ActuatorCreateDTO actuatorDto = new ActuatorCreateDTO();
        actuatorDto.setName("HomeBattery-" + suffix);
        actuatorDto.setType("battery");
        actuatorDto.setRoomId(roomId);
        actuatorDto.setTargetState("pending");
        actuatorsApi.createActuator(actuatorDto);

        await().atMost(Duration.ofSeconds(30)).until(actuatorExists(actuatorDto.getName()));
        long actuatorId = getActuatorIdByName(actuatorDto.getName());

        await().atMost(Duration.ofSeconds(90)).until(actuatorTargetBecomesOneOf(
                actuatorId,
                "import_to_100",
                "export_to_grid",
                "sell_excess"
        ));
    }

    protected Callable<Boolean> roomExists(String roomName) {
        return () -> roomsApi.getRooms().stream().anyMatch(r -> roomName.equals(r.getName()));
    }

    protected long getRoomIdByName(String roomName) {
        List<RoomDTO> rooms = roomsApi.getRooms();
        return rooms.stream()
                .filter(r -> roomName.equals(r.getName()))
                .findFirst()
                .orElseThrow()
                .getId();
    }

    protected Callable<Boolean> actuatorExists(String actuatorName) {
        return () -> actuatorsApi.getActuators().stream().anyMatch(a -> actuatorName.equals(a.getName()));
    }

    protected long getActuatorIdByName(String actuatorName) {
        List<ActuatorDTO> actuators = actuatorsApi.getActuators();
        return actuators.stream()
                .filter(a -> actuatorName.equals(a.getName()))
                .findFirst()
                .orElseThrow()
                .getId();
    }

    protected Callable<Boolean> actuatorTargetBecomesOneOf(long actuatorId, String... validStates) {
        return () -> {
            ActuatorDTO actuator = actuatorsApi.getActuator(actuatorId);
            if (actuator == null || actuator.getTargetState() == null) {
                return false;
            }

            for (String validState : validStates) {
                if (validState.equals(actuator.getTargetState())) {
                    return true;
                }
            }
            return false;
        };
    }
}