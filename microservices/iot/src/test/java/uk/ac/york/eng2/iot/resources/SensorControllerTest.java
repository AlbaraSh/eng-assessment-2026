package uk.ac.york.eng2.iot.resources;

import io.micronaut.http.HttpHeaders;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.york.eng2.iot.domain.Reading;
import uk.ac.york.eng2.iot.domain.Room;
import uk.ac.york.eng2.iot.domain.Sensor;
import uk.ac.york.eng2.iot.dto.ReadingDTO;
import uk.ac.york.eng2.iot.dto.RoomCreateDTO;
import uk.ac.york.eng2.iot.dto.SensorCreateDTO;
import uk.ac.york.eng2.iot.dto.SensorDTO;
import uk.ac.york.eng2.iot.dto.SensorUpdateDTO;
import uk.ac.york.eng2.iot.repository.ActuatorRepository;
import uk.ac.york.eng2.iot.repository.ReadingRepository;
import uk.ac.york.eng2.iot.repository.RoomRepository;
import uk.ac.york.eng2.iot.repository.SensorRepository;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest(transactional = false)
public class SensorControllerTest {

    @Inject
    private SensorClient client;

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
    public void listNoSensors() {
        HttpResponse<List<SensorDTO>> response = client.list();
        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals(0, response.body().size());
    }

    @Test
    public void createThenList() {
        long roomId = createRoom("Kitchen");
        long sensorId = createSensor("KitchenTemperature", "temperature", roomId);

        HttpResponse<List<SensorDTO>> response = client.list();
        assertEquals(HttpStatus.OK, response.getStatus());

        List<SensorDTO> sensors = response.body();
        assertEquals(1, sensors.size());

        SensorDTO sensor = sensors.get(0);
        assertEquals(sensorId, sensor.getId());
        assertEquals("KitchenTemperature", sensor.getName());
        assertEquals("temperature", sensor.getType());
        assertEquals(roomId, sensor.getRoomId());
    }

    @Test
    public void createThenGet() {
        long roomId = createRoom("Garage");
        long sensorId = createSensor("GarageTemperature", "temperature", roomId);

        HttpResponse<SensorDTO> response = client.get(sensorId);
        assertEquals(HttpStatus.OK, response.getStatus());

        SensorDTO sensor = response.body();
        assertEquals(sensorId, sensor.getId());
        assertEquals("GarageTemperature", sensor.getName());
        assertEquals("temperature", sensor.getType());
        assertEquals(roomId, sensor.getRoomId());
    }

    @Test
    public void createThenUpdateAndGet() {
        long roomId = createRoom("Bedroom");
        long newRoomId = createRoom("Living Room");
        long sensorId = createSensor("BedroomTemperature", "temperature", roomId);

        SensorUpdateDTO dto = new SensorUpdateDTO();
        dto.setName("LivingRoomPresence");
        dto.setType("presence");
        dto.setRoomId(newRoomId);

        HttpResponse<Void> updateResponse = client.update(dto, sensorId);
        assertEquals(HttpStatus.OK, updateResponse.getStatus());

        HttpResponse<SensorDTO> getResponse = client.get(sensorId);
        assertEquals(HttpStatus.OK, getResponse.getStatus());

        SensorDTO sensor = getResponse.body();
        assertEquals(sensorId, sensor.getId());
        assertEquals("LivingRoomPresence", sensor.getName());
        assertEquals("presence", sensor.getType());
        assertEquals(newRoomId, sensor.getRoomId());
    }

    @Test
    public void createThenDelete() {
        long roomId = createRoom("Office");
        long sensorId = createSensor("OfficeTemperature", "temperature", roomId);

        HttpResponse<Void> deleteResponse = client.delete(sensorId);
        assertEquals(HttpStatus.OK, deleteResponse.getStatus());

        HttpResponse<List<SensorDTO>> listResponse = client.list();
        assertEquals(HttpStatus.OK, listResponse.getStatus());
        assertEquals(0, listResponse.body().size());
    }

    @Test
    public void createDuplicateSensorFails() {
        long roomId = createRoom("Kitchen");
        createSensor("KitchenTemperature", "temperature", roomId);

        SensorCreateDTO dto = new SensorCreateDTO();
        dto.setName("KitchenTemperature");
        dto.setType("temperature");
        dto.setRoomId(roomId);

        HttpClientResponseException ex =
                assertThrows(HttpClientResponseException.class, () -> client.create(dto));
        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
    }

    @Test
    public void createSensorWithBlankNameFails() {
        long roomId = createRoom("Kitchen");

        SensorCreateDTO dto = new SensorCreateDTO();
        dto.setName(" ");
        dto.setType("temperature");
        dto.setRoomId(roomId);

        HttpClientResponseException ex =
                assertThrows(HttpClientResponseException.class, () -> client.create(dto));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    public void createSensorWithMissingRoomFails() {
        SensorCreateDTO dto = new SensorCreateDTO();
        dto.setName("KitchenTemperature");
        dto.setType("temperature");
        dto.setRoomId(-1L);

        HttpClientResponseException ex =
                assertThrows(HttpClientResponseException.class, () -> client.create(dto));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
    }

    @Test
    public void getMissingSensorFails() {
        HttpResponse<SensorDTO> response = client.get(-1L);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatus());
    }

    @Test
    public void updateMissingSensorFails() {
        long roomId = createRoom("Kitchen");

        SensorUpdateDTO dto = new SensorUpdateDTO();
        dto.setName("MissingSensor");
        dto.setType("temperature");
        dto.setRoomId(roomId);

        HttpResponse<Void> response = client.update(dto, -1L);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatus());
    }

    @Test
    public void deleteMissingSensorFails() {
        HttpResponse<Void> response = client.delete(-1L);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatus());
    }

    @Test
    public void getReadingsForMissingSensorFails() {
        HttpResponse<List<ReadingDTO>> response = client.getReadings(-1L);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatus());
    }

    @Test
    public void getSensorReadings() {
        Room room = new Room();
        room.setName("Kitchen");
        room = roomRepository.save(room);

        Sensor sensor = new Sensor();
        sensor.setName("KitchenTemperature");
        sensor.setType("temperature");
        sensor.setRoom(room);
        sensor = sensorRepository.save(sensor);

        Reading older = new Reading();
        older.setSensor(sensor);
        older.setTakenAt(Instant.parse("2026-01-01T10:00:00Z"));
        older.setValue(18.5);
        readingRepository.save(older);

        Reading newer = new Reading();
        newer.setSensor(sensor);
        newer.setTakenAt(Instant.parse("2026-01-01T11:00:00Z"));
        newer.setValue(19.0);
        readingRepository.save(newer);

        HttpResponse<List<ReadingDTO>> response = client.getReadings(sensor.getId());
        assertEquals(HttpStatus.OK, response.getStatus());

        List<ReadingDTO> readings = response.body();
        assertEquals(2, readings.size());

        assertEquals(sensor.getId(), readings.get(0).getSensorId());
        assertEquals(19.0, readings.get(0).getValue());
        assertEquals(sensor.getId(), readings.get(1).getSensorId());
        assertEquals(18.5, readings.get(1).getValue());
    }

    private long createRoom(String name) {
        RoomCreateDTO dto = new RoomCreateDTO();
        dto.setName(name);

        Room room = new Room();
        room.setName(name);
        room = roomRepository.save(room);
        return room.getId();
    }

    private long createSensor(String name, String type, long roomId) {
        SensorCreateDTO dto = new SensorCreateDTO();
        dto.setName(name);
        dto.setType(type);
        dto.setRoomId(roomId);

        HttpResponse<Void> response = client.create(dto);
        assertEquals(HttpStatus.CREATED, response.getStatus());
        return Long.parseLong(response.header(HttpHeaders.LOCATION).split("/")[2]);
    }
}