package uk.ac.york.eng2.iot.resources;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.york.eng2.iot.domain.Room;
import uk.ac.york.eng2.iot.dto.ActuatorCreateDTO;
import uk.ac.york.eng2.iot.dto.ActuatorDTO;
import uk.ac.york.eng2.iot.dto.ActuatorUpdateDTO;
import uk.ac.york.eng2.iot.repository.ActuatorRepository;
import uk.ac.york.eng2.iot.repository.ReadingRepository;
import uk.ac.york.eng2.iot.repository.RoomRepository;
import uk.ac.york.eng2.iot.repository.SensorRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest(transactional = false)
public class ActuatorControllerTest {

    @Inject
    private ActuatorClient client;

    @Inject
    private RoomRepository roomRepository;

    @Inject
    private SensorRepository sensorRepository;

    @Inject
    private ActuatorRepository actuatorRepository;

    @Inject
    private ReadingRepository readingRepository;

    @BeforeEach
    public void setUp() {
        readingRepository.deleteAll();
        sensorRepository.deleteAll();
        actuatorRepository.deleteAll();
        roomRepository.deleteAll();
    }

    @Test
    public void listNoActuators() {
        HttpResponse<List<ActuatorDTO>> response = client.list();
        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals(0, response.body().size());
    }

    @Test
    public void createThenList() {
        long roomId = createRoom("Utility Room");
        long actuatorId = createActuator("HomeBattery", "battery", roomId, "sell_excess");

        HttpResponse<List<ActuatorDTO>> response = client.list();
        assertEquals(HttpStatus.OK, response.getStatus());

        List<ActuatorDTO> actuators = response.body();
        assertEquals(1, actuators.size());

        ActuatorDTO actuator = actuators.get(0);
        assertEquals(actuatorId, actuator.getId());
        assertEquals("HomeBattery", actuator.getName());
        assertEquals("battery", actuator.getType());
        assertEquals(roomId, actuator.getRoomId());
        assertEquals("sell_excess", actuator.getTargetState());
    }

    @Test
    public void createThenGet() {
        long roomId = createRoom("Garage");
        long actuatorId = createActuator("GarageBattery", "battery", roomId, "sell_excess");

        HttpResponse<ActuatorDTO> response = client.get(actuatorId);
        assertEquals(HttpStatus.OK, response.getStatus());

        ActuatorDTO actuator = response.body();
        assertEquals(actuatorId, actuator.getId());
        assertEquals("GarageBattery", actuator.getName());
        assertEquals("battery", actuator.getType());
        assertEquals(roomId, actuator.getRoomId());
        assertEquals("sell_excess", actuator.getTargetState());
    }

    @Test
    public void createThenUpdateAndGet() {
        long roomId = createRoom("Garage");
        long newRoomId = createRoom("Utility Room");
        long actuatorId = createActuator("HomeBattery", "battery", roomId, "sell_excess");

        ActuatorUpdateDTO dto = new ActuatorUpdateDTO();
        dto.setName("MainBattery");
        dto.setType("battery");
        dto.setRoomId(newRoomId);
        dto.setTargetState("import_to_100");

        HttpResponse<Void> updateResponse = client.update(dto, actuatorId);
        assertEquals(HttpStatus.OK, updateResponse.getStatus());

        HttpResponse<ActuatorDTO> getResponse = client.get(actuatorId);
        assertEquals(HttpStatus.OK, getResponse.getStatus());

        ActuatorDTO actuator = getResponse.body();
        assertEquals(actuatorId, actuator.getId());
        assertEquals("MainBattery", actuator.getName());
        assertEquals("battery", actuator.getType());
        assertEquals(newRoomId, actuator.getRoomId());
        assertEquals("import_to_100", actuator.getTargetState());
    }

    @Test
    public void createThenDelete() {
        long roomId = createRoom("Garage");
        long actuatorId = createActuator("HomeBattery", "battery", roomId, "sell_excess");

        HttpResponse<Void> deleteResponse = client.delete(actuatorId);
        assertEquals(HttpStatus.OK, deleteResponse.getStatus());

        HttpResponse<List<ActuatorDTO>> listResponse = client.list();
        assertEquals(HttpStatus.OK, listResponse.getStatus());
        assertEquals(0, listResponse.body().size());
    }

    @Test
    public void createDuplicateActuatorFails() {
        long roomId = createRoom("Garage");
        createActuator("HomeBattery", "battery", roomId, "sell_excess");

        ActuatorCreateDTO dto = new ActuatorCreateDTO();
        dto.setName("HomeBattery");
        dto.setType("battery");
        dto.setRoomId(roomId);
        dto.setTargetState("sell_excess");

        HttpClientResponseException ex =
                assertThrows(HttpClientResponseException.class, () -> client.create(dto));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    public void createActuatorWithBlankTargetStateFails() {
        long roomId = createRoom("Garage");

        ActuatorCreateDTO dto = new ActuatorCreateDTO();
        dto.setName("HomeBattery");
        dto.setType("battery");
        dto.setRoomId(roomId);
        dto.setTargetState(" ");

        HttpClientResponseException ex =
                assertThrows(HttpClientResponseException.class, () -> client.create(dto));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    public void createActuatorWithMissingRoomFails() {
        ActuatorCreateDTO dto = new ActuatorCreateDTO();
        dto.setName("HomeBattery");
        dto.setType("battery");
        dto.setRoomId(-1L);
        dto.setTargetState("sell_excess");

        HttpClientResponseException ex =
                assertThrows(HttpClientResponseException.class, () -> client.create(dto));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    public void getMissingActuatorFails() {
        HttpResponse<ActuatorDTO> response = client.get(-1L);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatus());
    }

    @Test
    public void updateMissingActuatorFails() {
        long roomId = createRoom("Garage");

        ActuatorUpdateDTO dto = new ActuatorUpdateDTO();
        dto.setName("MissingBattery");
        dto.setType("battery");
        dto.setRoomId(roomId);
        dto.setTargetState("import_to_100");

        HttpResponse<Void> response = client.update(dto, -1L);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatus());
    }

    @Test
    public void deleteMissingActuatorFails() {
        HttpResponse<Void> response = client.delete(-1L);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatus());
    }

    private long createRoom(String name) {
        Room room = new Room();
        room.setName(name);
        room = roomRepository.save(room);
        return room.getId();
    }

    private long createActuator(String name, String type, long roomId, String targetState) {
        ActuatorCreateDTO dto = new ActuatorCreateDTO();
        dto.setName(name);
        dto.setType(type);
        dto.setRoomId(roomId);
        dto.setTargetState(targetState);

        HttpResponse<Void> response = client.create(dto);
        assertEquals(HttpStatus.CREATED, response.getStatus());
        return Long.parseLong(response.header(HttpHeaders.LOCATION).split("/")[2]);
    }
}