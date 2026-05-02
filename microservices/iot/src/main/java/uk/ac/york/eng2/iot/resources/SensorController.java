package uk.ac.york.eng2.iot.resources;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import uk.ac.york.eng2.iot.domain.Room;
import uk.ac.york.eng2.iot.domain.Sensor;
import uk.ac.york.eng2.iot.dto.ReadingDTO;
import uk.ac.york.eng2.iot.dto.SensorCreateDTO;
import uk.ac.york.eng2.iot.dto.SensorDTO;
import uk.ac.york.eng2.iot.dto.SensorUpdateDTO;
import uk.ac.york.eng2.iot.mapper.IoTMMapper;
import uk.ac.york.eng2.iot.mapper.IoTMRequestMapper;
import uk.ac.york.eng2.iot.repository.ReadingRepository;
import uk.ac.york.eng2.iot.repository.RoomRepository;
import uk.ac.york.eng2.iot.repository.SensorRepository;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ExecuteOn(TaskExecutors.BLOCKING)
@Tag(name = "sensors")
@Controller("/sensors")
public class SensorController {

    @Inject
    private SensorRepository repository;

    @Inject
    private RoomRepository roomRepository;

    @Inject
    private ReadingRepository readingRepository;

    @Get
    public List<SensorDTO> getSensors() {
        return repository.findAll().stream()
                .map(IoTMMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Get("/{id}")
    public SensorDTO getSensor(@PathVariable long id) {
        @NonNull Optional<Sensor> oSensor = repository.findById(id);
        if (oSensor.isEmpty()) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Sensor not found");
        }

        return IoTMMapper.toDTO(oSensor.get());
    }

    @Post
    public HttpResponse<Void> createSensor(@Body SensorCreateDTO dto) {
        validateSensorDTO(dto);

        if (repository.existsByName(dto.getName())) {
            throw new HttpStatusException(HttpStatus.CONFLICT, "Sensor name already exists");
        }

        Room room = getRoom(dto.getRoomId());

        Sensor sensor = new Sensor();
        IoTMRequestMapper.apply(dto, sensor, room);

        sensor = repository.save(sensor);
        return HttpResponse.created(URI.create("/sensors/" + sensor.getId()));
    }

    @Transactional
    @Put("/{id}")
    public void updateSensor(@Body SensorUpdateDTO dto, @PathVariable long id) {
        validateSensorDTO(dto);

        @NonNull Optional<Sensor> oSensor = repository.findById(id);
        if (oSensor.isEmpty()) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Sensor not found");
        }

        Sensor sensor = oSensor.get();

        if (!sensor.getName().equals(dto.getName()) && repository.existsByName(dto.getName())) {
            throw new HttpStatusException(HttpStatus.CONFLICT, "Sensor name already exists");
        }

        Room room = getRoom(dto.getRoomId());

        IoTMRequestMapper.apply(dto, sensor, room);
        repository.save(sensor);
    }

    @Delete("/{id}")
    public void deleteSensor(@PathVariable long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
        } else {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Sensor not found");
        }
    }

    @Get("/{id}/readings")
    public List<ReadingDTO> getSensorReadings(@PathVariable long id) {
        if (!repository.existsById(id)) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Sensor not found");
        }

        return readingRepository.findBySensorIdOrderByTakenAtDesc(id).stream()
                .map(IoTMMapper::toDTO)
                .collect(Collectors.toList());
    }

    protected void validateSensorDTO(SensorCreateDTO dto) {
        if (dto == null) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Sensor name is required");
        }
        if (dto.getType() == null || dto.getType().trim().isEmpty()) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Sensor type is required");
        }
        if (dto.getRoomId() == null) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Room id is required");
        }
    }

    protected void validateSensorDTO(SensorUpdateDTO dto) {
        if (dto == null) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Sensor name is required");
        }
        if (dto.getType() == null || dto.getType().trim().isEmpty()) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Sensor type is required");
        }
        if (dto.getRoomId() == null) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Room id is required");
        }
    }

    protected Room getRoom(Long roomId) {
        @NonNull Optional<Room> oRoom = roomRepository.findById(roomId);
        if (oRoom.isEmpty()) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Room not found");
        }
        return oRoom.get();
    }
}