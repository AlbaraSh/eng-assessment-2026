package uk.ac.york.eng2.iot.resources;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.http.client.annotation.Client;
import uk.ac.york.eng2.iot.dto.RoomCreateDTO;
import uk.ac.york.eng2.iot.dto.RoomDTO;
import uk.ac.york.eng2.iot.dto.RoomUpdateDTO;

import java.util.List;

@Client("/rooms")
public interface RoomClient {

    @Get
    HttpResponse<List<RoomDTO>> list();

    @Get("/{id}")
    HttpResponse<RoomDTO> get(@PathVariable long id);

    @Post
    HttpResponse<Void> create(@Body RoomCreateDTO dto);

    @Put("/{id}")
    HttpResponse<Void> update(@Body RoomUpdateDTO dto, @PathVariable long id);

    @Delete("/{id}")
    HttpResponse<Void> delete(@PathVariable long id);
}