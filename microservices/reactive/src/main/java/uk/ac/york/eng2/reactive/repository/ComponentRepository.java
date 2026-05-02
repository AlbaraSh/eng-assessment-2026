package uk.ac.york.eng2.reactive.repository;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.PageableRepository;
import uk.ac.york.eng2.reactive.domain.Component;

import java.util.Optional;

@Repository
public interface ComponentRepository extends PageableRepository<Component, Long> {

    boolean existsByName(String name);
    Optional<Component> findByName(String name);
}