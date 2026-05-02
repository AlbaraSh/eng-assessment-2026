package uk.ac.york.eng2.iot.resources;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.*;
import io.micronaut.http.client.annotation.Client;
import uk.ac.york.eng2.iot.dto.ReadingDTO;
import uk.ac.york.eng2.iot.dto.SensorCreateDTO;
import uk.ac.york.eng2.iot.dto.SensorDTO;
import uk.ac.york.eng2.iot.dto.SensorUpdateDTO;

import java.util.List;

@Client("/sensors")
public interface SensorClient {

    @Get
    HttpResponse<List<SensorDTO>> list();

    @Get("/{id}")
    HttpResponse<SensorDTO> get(@PathVariable long id);

    @Get("/{id}/readings")
    HttpResponse<List<ReadingDTO>> getReadings(@PathVariable long id);

    @Post
    HttpResponse<Void> create(@Body SensorCreateDTO dto);

    @Put("/{id}")
    HttpResponse<Void> update(@Body SensorUpdateDTO dto, @PathVariable long id);

    @Delete("/{id}")
    HttpResponse<Void> delete(@PathVariable long id);
}