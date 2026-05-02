package uk.ac.york.eng2.iot.repository;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.PageableRepository;
import uk.ac.york.eng2.iot.domain.Room;

@Repository
public interface RoomRepository extends PageableRepository<Room, Long> {
    boolean existsByName(String name);
}