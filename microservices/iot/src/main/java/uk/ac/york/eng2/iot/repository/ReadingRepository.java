package uk.ac.york.eng2.iot.repository;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.PageableRepository;
import uk.ac.york.eng2.iot.domain.Reading;

import java.util.List;

@Repository
public interface ReadingRepository extends PageableRepository<Reading, Long> {

    List<Reading> findBySensorId(Long sensorId);
    List<Reading> findBySensorIdOrderByTakenAtDesc(Long sensorId);
}