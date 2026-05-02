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
import uk.ac.york.eng2.iot.dto.RoomCreateDTO;
import uk.ac.york.eng2.iot.dto.RoomDTO;
import uk.ac.york.eng2.iot.dto.RoomUpdateDTO;
import uk.ac.york.eng2.iot.repository.ActuatorRepository;
import uk.ac.york.eng2.iot.repository.ReadingRepository;
import uk.ac.york.eng2.iot.repository.RoomRepository;
import uk.ac.york.eng2.iot.repository.SensorRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest(transactional = false)
public class RoomControllerTest {

    @Inject
    private RoomClient client;

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
    public void listNoRooms() {
        HttpResponse<List<RoomDTO>> response = client.list();
        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals(0, response.body().size());
    }

    @Test
    public void createThenList() {
        RoomCreateDTO dto = new RoomCreateDTO();
        dto.setName("Kitchen");
        long id = createRoom(dto);

        HttpResponse<List<RoomDTO>> response = client.list();
        assertEquals(HttpStatus.OK, response.getStatus());

        List<RoomDTO> rooms = response.body();
        assertEquals(1, rooms.size());

        RoomDTO room = rooms.get(0);
        assertEquals(id, room.getId());
        assertEquals("Kitchen", room.getName());
    }

    @Test
    public void createThenGet() {
        RoomCreateDTO dto = new RoomCreateDTO();
        dto.setName("Garage");
        long id = createRoom(dto);

        HttpResponse<RoomDTO> response = client.get(id);
        assertEquals(HttpStatus.OK, response.getStatus());

        RoomDTO room = response.body();
        assertEquals(id, room.getId());
        assertEquals("Garage", room.getName());
    }

    @Test
    public void createThenUpdateAndGet() {
        RoomCreateDTO createDTO = new RoomCreateDTO();
        createDTO.setName("Bedroom");
        long id = createRoom(createDTO);

        RoomUpdateDTO updateDTO = new RoomUpdateDTO();
        updateDTO.setName("Main Bedroom");

        HttpResponse<Void> updateResponse = client.update(updateDTO, id);
        assertEquals(HttpStatus.OK, updateResponse.getStatus());

        HttpResponse<RoomDTO> getResponse = client.get(id);
        assertEquals(HttpStatus.OK, getResponse.getStatus());

        RoomDTO updated = getResponse.body();
        assertEquals(id, updated.getId());
        assertEquals("Main Bedroom", updated.getName());
    }

    @Test
    public void createThenDelete() {
        RoomCreateDTO dto = new RoomCreateDTO();
        dto.setName("Office");
        long id = createRoom(dto);

        HttpResponse<Void> deleteResponse = client.delete(id);
        assertEquals(HttpStatus.OK, deleteResponse.getStatus());

        HttpResponse<List<RoomDTO>> listResponse = client.list();
        assertEquals(HttpStatus.OK, listResponse.getStatus());
        assertEquals(0, listResponse.body().size());
    }

    @Test
    public void createDuplicateRoomFails() {
        RoomCreateDTO dto = new RoomCreateDTO();
        dto.setName("Kitchen");
        createRoom(dto);

        HttpClientResponseException ex =
                assertThrows(HttpClientResponseException.class, () -> client.create(dto));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    public void createRoomWithBlankNameFails() {
        RoomCreateDTO dto = new RoomCreateDTO();
        dto.setName(" ");

        HttpClientResponseException ex =
                assertThrows(HttpClientResponseException.class, () -> client.create(dto));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    public void getMissingRoomFails() {
        HttpResponse<RoomDTO> response = client.get(-1L);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatus());
    }

    @Test
    public void updateMissingRoomFails() {
        RoomUpdateDTO dto = new RoomUpdateDTO();
        dto.setName("Nowhere");

        HttpResponse<Void> response = client.update(dto, -1L);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatus());
    }

    @Test
    public void deleteMissingRoomFails() {
        HttpResponse<Void> response = client.delete(-1L);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatus());
    }

    @Test
    public void repositoryAndControllerSeeSameData() {
        Room room = new Room();
        room.setName("Living Room");
        room = roomRepository.save(room);

        HttpResponse<RoomDTO> response = client.get(room.getId());
        assertEquals(HttpStatus.OK, response.getStatus());

        RoomDTO fetched = response.body();
        assertEquals(room.getId(), fetched.getId());
        assertEquals("Living Room", fetched.getName());
    }

    private long createRoom(RoomCreateDTO dto) {
        HttpResponse<Void> response = client.create(dto);
        assertEquals(HttpStatus.CREATED, response.getStatus());
        return Long.parseLong(response.header(HttpHeaders.LOCATION).split("/")[2]);
    }
}