package uk.ac.york.eng2.iot.repository;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.PageableRepository;
import uk.ac.york.eng2.iot.domain.Actuator;

import java.util.List;

@Repository
public interface ActuatorRepository extends PageableRepository<Actuator, Long> {

    boolean existsByName(String name);
    List<Actuator> findByRoomId(Long roomId);
    List<Actuator> findByType(String type);
}