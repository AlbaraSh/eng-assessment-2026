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
import uk.ac.york.eng2.iot.domain.Actuator;
import uk.ac.york.eng2.iot.domain.Room;
import uk.ac.york.eng2.iot.dto.ActuatorCreateDTO;
import uk.ac.york.eng2.iot.dto.ActuatorDTO;
import uk.ac.york.eng2.iot.dto.ActuatorUpdateDTO;
import uk.ac.york.eng2.iot.mapper.IoTMMapper;
import uk.ac.york.eng2.iot.mapper.IoTMRequestMapper;
import uk.ac.york.eng2.iot.repository.ActuatorRepository;
import uk.ac.york.eng2.iot.repository.RoomRepository;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ExecuteOn(TaskExecutors.BLOCKING)
@Tag(name = "actuators")
@Controller("/actuators")
public class ActuatorController {

    @Inject
    private ActuatorRepository repository;

    @Inject
    private RoomRepository roomRepository;

    @Get
    public List<ActuatorDTO> getActuators() {
        return repository.findAll().stream()
                .map(IoTMMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Get("/{id}")
    public ActuatorDTO getActuator(@PathVariable long id) {
        @NonNull Optional<Actuator> oActuator = repository.findById(id);
        if (oActuator.isEmpty()) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Actuator not found");
        }

        return IoTMMapper.toDTO(oActuator.get());
    }

    @Post
    public HttpResponse<Void> createActuator(@Body ActuatorCreateDTO dto) {
        validateActuatorDTO(dto);

        if (repository.existsByName(dto.getName())) {
            throw new HttpStatusException(HttpStatus.CONFLICT, "Actuator name already exists");
        }

        Room room = getRoom(dto.getRoomId());

        Actuator actuator = new Actuator();
        IoTMRequestMapper.apply(dto, actuator, room);

        actuator = repository.save(actuator);
        return HttpResponse.created(URI.create("/actuators/" + actuator.getId()));
    }

    @Transactional
    @Put("/{id}")
    public void updateActuator(@Body ActuatorUpdateDTO dto, @PathVariable long id) {
        validateActuatorDTO(dto);

        @NonNull Optional<Actuator> oActuator = repository.findById(id);
        if (oActuator.isEmpty()) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Actuator not found");
        }

        Actuator actuator = oActuator.get();

        if (!actuator.getName().equals(dto.getName()) && repository.existsByName(dto.getName())) {
            throw new HttpStatusException(HttpStatus.CONFLICT, "Actuator name already exists");
        }

        Room room = getRoom(dto.getRoomId());

        IoTMRequestMapper.apply(dto, actuator, room);
        repository.save(actuator);
    }

    @Delete("/{id}")
    public void deleteActuator(@PathVariable long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
        } else {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Actuator not found");
        }
    }

    protected void validateActuatorDTO(ActuatorCreateDTO dto) {
        if (dto == null) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Actuator name is required");
        }
        if (dto.getType() == null || dto.getType().trim().isEmpty()) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Actuator type is required");
        }
        if (dto.getTargetState() == null || dto.getTargetState().trim().isEmpty()) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Target state is required");
        }
        if (dto.getRoomId() == null) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Room id is required");
        }
    }

    protected void validateActuatorDTO(ActuatorUpdateDTO dto) {
        if (dto == null) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (dto.getName() == null || dto.getName().trim().isEmpty()) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Actuator name is required");
        }
        if (dto.getType() == null || dto.getType().trim().isEmpty()) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Actuator type is required");
        }
        if (dto.getTargetState() == null || dto.getTargetState().trim().isEmpty()) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Target state is required");
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
