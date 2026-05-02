package uk.ac.york.eng2.reactive.repository;

import io.micronaut.data.annotation.Repository;
import io.micronaut.data.repository.PageableRepository;
import uk.ac.york.eng2.reactive.domain.TopicSlot;

import java.util.List;
import java.util.Optional;

@Repository
public interface TopicSlotRepository extends PageableRepository<TopicSlot, Long> {

    boolean existsByTopicNameAndSlotName(String topicName, String slotName);
    Optional<TopicSlot> findByTopicNameAndSlotName(String topicName, String slotName);
    List<TopicSlot> findByTopicName(String topicName);
}