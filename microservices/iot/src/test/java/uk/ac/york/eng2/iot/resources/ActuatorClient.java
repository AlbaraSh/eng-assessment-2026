package uk.ac.york.eng2.iot.resources;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.http.client.annotation.Client;
import uk.ac.york.eng2.iot.dto.ActuatorCreateDTO;
import uk.ac.york.eng2.iot.dto.ActuatorDTO;
import uk.ac.york.eng2.iot.dto.ActuatorUpdateDTO;

import java.util.List;

@Client("/actuators")
public interface ActuatorClient {

    @Get
    HttpResponse<List<ActuatorDTO>> list();

    @Get("/{id}")
    HttpResponse<ActuatorDTO> get(@PathVariable long id);

    @Post
    HttpResponse<Void> create(@Body ActuatorCreateDTO dto);

    @Put("/{id}")
    HttpResponse<Void> update(@Body ActuatorUpdateDTO dto, @PathVariable long id);

    @Delete("/{id}")
    HttpResponse<Void> delete(@PathVariable long id);
}