package uk.ac.york.eng2.iot.repository;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.PageableRepository;
import uk.ac.york.eng2.iot.domain.Sensor;

import java.util.List;

@Repository
public interface SensorRepository extends PageableRepository<Sensor, Long> {

    boolean existsByName(String name);
    List<Sensor> findByRoomId(Long roomId);
    List<Sensor> findByType(String type);
}
