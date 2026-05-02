package uk.ac.york.eng2.reactive.resources;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.http.client.annotation.Client;
import uk.ac.york.eng2.reactive.dto.ComponentDTO;

import java.util.List;

@Client("/components")
public interface ComponentClient {

    @Get
    HttpResponse<List<ComponentDTO>> list();

    @Get("/{id}")
    HttpResponse<ComponentDTO> get(@PathVariable long id);
}