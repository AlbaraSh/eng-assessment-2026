package uk.ac.york.eng2.reactive.resources;

import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import uk.ac.york.eng2.reactive.domain.Component;
import uk.ac.york.eng2.reactive.dto.ComponentDTO;
import uk.ac.york.eng2.reactive.repository.ComponentRepository;
import uk.ac.york.eng2.reactive.repository.TopicSlotRepository;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@MicronautTest(transactional = false)
public class ComponentControllerTest {

    @Inject
    private ComponentClient client;

    @Inject
    private ComponentRepository componentRepository;

    @Inject
    private TopicSlotRepository topicSlotRepository;

    @BeforeEach
    public void setUp() {
        topicSlotRepository.deleteAll();
        componentRepository.deleteAll();
    }

    @Test
    public void listNoComponents() {
        HttpResponse<List<ComponentDTO>> response = client.list();
        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals(0, response.body().size());
    }

    @Test
    public void createThenList() {
        Component component = new Component();
        component.setName("AgileRatesFetcher");
        component = componentRepository.save(component);

        HttpResponse<List<ComponentDTO>> response = client.list();
        assertEquals(HttpStatus.OK, response.getStatus());

        List<ComponentDTO> components = response.body();
        assertEquals(1, components.size());

        ComponentDTO dto = components.get(0);
        assertEquals(component.getId(), dto.getId());
        assertEquals("AgileRatesFetcher", dto.getName());
    }

    @Test
    public void createThenGet() {
        Component component = new Component();
        component.setName("BatteryDecisionComponent");
        component = componentRepository.save(component);

        HttpResponse<ComponentDTO> response = client.get(component.getId());
        assertEquals(HttpStatus.OK, response.getStatus());

        ComponentDTO dto = response.body();
        assertEquals(component.getId(), dto.getId());
        assertEquals("BatteryDecisionComponent", dto.getName());
    }

    @Test
    public void getMissingComponentFails() {
        HttpResponse<ComponentDTO> response = client.get(-1L);
        assertEquals(HttpStatus.NOT_FOUND, response.getStatus());
    }

    @Test
    public void repositoryAndControllerSeeSameData() {
        Component component = new Component();
        component.setName("CurrentImportRateExtractor");
        component = componentRepository.save(component);

        HttpResponse<ComponentDTO> response = client.get(component.getId());
        assertEquals(HttpStatus.OK, response.getStatus());

        ComponentDTO dto = response.body();
        assertEquals(component.getId(), dto.getId());
        assertEquals("CurrentImportRateExtractor", dto.getName());
    }
}