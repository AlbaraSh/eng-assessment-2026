package uk.ac.york.eng2.reactive.resources;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import uk.ac.york.eng2.reactive.domain.Component;
import uk.ac.york.eng2.reactive.dto.ComponentDTO;
import uk.ac.york.eng2.reactive.mapper.RCMMapper;
import uk.ac.york.eng2.reactive.repository.ComponentRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ExecuteOn(TaskExecutors.BLOCKING)
@Tag(name = "components")
@Controller("/components")
public class ComponentController {

    @Inject
    private ComponentRepository repository;

    @Get
    public List<ComponentDTO> getComponents() {
        return repository.findAll().stream()
                .map(RCMMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Get("/{id}")
    public ComponentDTO getComponent(@PathVariable long id) {
        @NonNull Optional<Component> oComponent = repository.findById(id);
        if (oComponent.isEmpty()) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Component not found");
        }

        return RCMMapper.toDTO(oComponent.get());
    }
}