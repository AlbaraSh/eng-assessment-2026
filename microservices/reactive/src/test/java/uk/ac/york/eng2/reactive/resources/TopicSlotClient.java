package uk.ac.york.eng2.reactive.resources;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.http.client.annotation.Client;
import uk.ac.york.eng2.reactive.dto.TopicSlotDTO;

import java.util.List;

@Client("/topic-slots")
public interface TopicSlotClient {

    @Get
    HttpResponse<List<TopicSlotDTO>> list();

    @Get("/{id}")
    HttpResponse<TopicSlotDTO> get(@PathVariable long id);

    @Get("/topic/{topicName}")
    HttpResponse<List<TopicSlotDTO>> getByTopic(@PathVariable String topicName);
}