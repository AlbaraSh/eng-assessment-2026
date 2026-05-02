package uk.ac.york.eng2.reactive.resources;

import io.micronaut.core.annotation.NonNull;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.annotation.*;
import io.micronaut.http.exceptions.HttpStatusException;
import io.micronaut.scheduling.TaskExecutors;
import io.micronaut.scheduling.annotation.ExecuteOn;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.inject.Inject;
import uk.ac.york.eng2.reactive.domain.TopicSlot;
import uk.ac.york.eng2.reactive.dto.TopicSlotDTO;
import uk.ac.york.eng2.reactive.mapper.RCMMapper;
import uk.ac.york.eng2.reactive.repository.TopicSlotRepository;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@ExecuteOn(TaskExecutors.BLOCKING)
@Tag(name = "topic-slots")
@Controller("/topic-slots")
public class TopicSlotController {

    @Inject
    private TopicSlotRepository repository;

    @Get
    public List<TopicSlotDTO> getTopicSlots() {
        return repository.findAll().stream()
                .map(RCMMapper::toDTO)
                .collect(Collectors.toList());
    }

    @Get("/{id}")
    public TopicSlotDTO getTopicSlot(@PathVariable long id) {
        @NonNull Optional<TopicSlot> oTopicSlot = repository.findById(id);
        if (oTopicSlot.isEmpty()) {
            throw new HttpStatusException(HttpStatus.NOT_FOUND, "Topic slot not found");
        }

        return RCMMapper.toDTO(oTopicSlot.get());
    }

    @Get("/topic/{topicName}")
    public List<TopicSlotDTO> getTopicSlotsForTopic(@PathVariable String topicName) {
        return repository.findByTopicName(topicName).stream()
                .map(RCMMapper::toDTO)
                .collect(Collectors.toList());
    }
}