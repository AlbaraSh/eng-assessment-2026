package uk.ac.york.eng2.reactive.service;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import uk.ac.york.eng2.reactive.domain.Component;
import uk.ac.york.eng2.reactive.repository.ComponentRepository;

import java.util.List;
import java.util.Optional;

@Singleton
public class ComponentService {

    @Inject
    private ComponentRepository repository;

    public List<Component> listAll() {
        return repository.findAll();
    }

    public Optional<Component> findById(long id) {
        return repository.findById(id);
    }

    public Optional<Component> findByName(String name) {
        return repository.findByName(name);
    }

    @Transactional
    public Component ensureExists(String name) {
        Optional<Component> existing = repository.findByName(name);
        if (existing.isPresent()) {
            return existing.get();
        }

        Component component = new Component();
        component.setName(name);
        return repository.save(component);
    }
}