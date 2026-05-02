package uk.ac.york.eng2.reactive.service;

import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import jakarta.transaction.Transactional;
import uk.ac.york.eng2.reactive.domain.TopicSlot;
import uk.ac.york.eng2.reactive.repository.TopicSlotRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Singleton
public class TopicSlotService {

    @Inject
    private TopicSlotRepository repository;

    public List<TopicSlot> listAll() {
        return repository.findAll();
    }

    public Optional<TopicSlot> findById(long id) {
        return repository.findById(id);
    }

    public List<TopicSlot> findByTopicName(String topicName) {
        return repository.findByTopicName(topicName);
    }

    public Optional<TopicSlot> findByTopicAndSlot(String topicName, String slotName) {
        return repository.findByTopicNameAndSlotName(topicName, slotName);
    }

    public Optional<Double> getDoubleValue(String topicName, String slotName) {
        return repository.findByTopicNameAndSlotName(topicName, slotName)
                .map(TopicSlot::getDoubleValue);
    }

    public Optional<Long> getLongValue(String topicName, String slotName) {
        return repository.findByTopicNameAndSlotName(topicName, slotName)
                .map(TopicSlot::getLongValue);
    }

    public Optional<String> getTextValue(String topicName, String slotName) {
        return repository.findByTopicNameAndSlotName(topicName, slotName)
                .map(TopicSlot::getTextValue);
    }

    public Optional<Instant> getTimestampValue(String topicName, String slotName) {
        return repository.findByTopicNameAndSlotName(topicName, slotName)
                .map(TopicSlot::getTimestampValue);
    }

    @Transactional
    public TopicSlot saveDoubleSlot(String topicName, String slotName, Double value) {
        TopicSlot slot = getOrCreate(topicName, slotName);
        slot.setSlotType("double");
        clearValues(slot);
        slot.setDoubleValue(value);
        return repository.save(slot);
    }

    @Transactional
    public TopicSlot saveLongSlot(String topicName, String slotName, Long value) {
        TopicSlot slot = getOrCreate(topicName, slotName);
        slot.setSlotType("long");
        clearValues(slot);
        slot.setLongValue(value);
        return repository.save(slot);
    }

    @Transactional
    public TopicSlot saveTextSlot(String topicName, String slotName, String value) {
        TopicSlot slot = getOrCreate(topicName, slotName);
        slot.setSlotType("text");
        clearValues(slot);
        slot.setTextValue(value);
        return repository.save(slot);
    }

    @Transactional
    public TopicSlot saveTimestampSlot(String topicName, String slotName, Instant value) {
        TopicSlot slot = getOrCreate(topicName, slotName);
        slot.setSlotType("timestamp");
        clearValues(slot);
        slot.setTimestampValue(value);
        return repository.save(slot);
    }

    protected TopicSlot getOrCreate(String topicName, String slotName) {
        Optional<TopicSlot> existing = repository.findByTopicNameAndSlotName(topicName, slotName);
        if (existing.isPresent()) {
            return existing.get();
        }

        TopicSlot slot = new TopicSlot();
        slot.setTopicName(topicName);
        slot.setSlotName(slotName);
        return slot;
    }

    protected void clearValues(TopicSlot slot) {
        slot.setTimestampValue(null);
        slot.setTextValue(null);
        slot.setDoubleValue(null);
        slot.setLongValue(null);
    }
}