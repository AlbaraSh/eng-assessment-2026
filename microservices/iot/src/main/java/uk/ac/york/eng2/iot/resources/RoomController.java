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
import uk.ac.york.eng2.iot.dto.RoomCreateDTO;
import uk.ac.york.eng2.iot.dto.RoomDTO;
import uk.ac.york.eng2.iot.dto.RoomUpdateDTO;
import uk.ac.york.eng2.iot.mapper.IoTMMapper;
import uk.ac.york.eng2.iot.mapper.IoTMRequestMapper;
import uk.ac.york.eng2.iot.repository.RoomRepository;

import java.net.URI;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ExecuteOn(TaskExecutors.BLOCKING)
@Tag(name = "rooms")
@Controller("/rooms")
public class RoomController {

    @Inject
    private RoomRepository repository;

    @Get
    public List<RoomDTO> getRooms() {
        return repository.findAll().stream()
                .map(IoTMMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Get("/{id}")
    public RoomDTO getRoom(@PathVariable long id) {
        @NonNull Optional<Room> oRoom = repository.findById(id);
        if (oRoom.isEmpty()) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }
        return IoTMMapper.toDTO(oRoom.get());
    }

    @Post
    public HttpResponse<Void> createRoom(@Body RoomCreateDTO dto) {
        validateRoomName(dto.getName());

        if (repository.existsByName(dto.getName())) {
            throw new HttpStatusException(HttpStatus.CONFLICT, "Room name already exists");
        }

        Room room = new Room();
        IoTMRequestMapper.apply(dto, room);

        room = repository.save(room);
        return HttpResponse.created(URI.create("/rooms/" + room.getId()));
    }

    @Transactional
    @Put("/{id}")
    public void updateRoom(@Body RoomUpdateDTO dto, @PathVariable long id) {
        validateRoomName(dto.getName());

        @NonNull Optional<Room> oRoom = repository.findById(id);
        if (oRoom.isEmpty()) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }

        Room room = oRoom.get();

        if (!room.getName().equals(dto.getName()) && repository.existsByName(dto.getName())) {
            throw new HttpStatusException(HttpStatus.CONFLICT, "Room name already exists");
        }

        IoTMRequestMapper.apply(dto, room);
        repository.save(room);
    }

    @Delete("/{id}")
    public void deleteRoom(@PathVariable long id) {
        if (repository.existsById(id)) {
            repository.deleteById(id);
        } else {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Room not found");
        }
    }

    protected void validateRoomName(String name) {
        if (name == null || name.trim().isEmpty()) {
            throw new HttpStatusException(HttpStatus.BAD_REQUEST, "Room name is required");
        }
    }
}