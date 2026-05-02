package uk.ac.york.eng2.reactive.mapper;

import uk.ac.york.eng2.reactive.domain.Component;
import uk.ac.york.eng2.reactive.domain.TopicSlot;
import uk.ac.york.eng2.reactive.dto.ComponentDTO;
import uk.ac.york.eng2.reactive.dto.TopicSlotDTO;

public class RCMMapper {

    private RCMMapper() {
    }

    public static ComponentDTO toDTO(Component component) {
        if (component == null) {
            return null;
        }

        ComponentDTO dto = new ComponentDTO();
        dto.setId(component.getId());
        dto.setName(component.getName());
        return dto;
    }

    public static TopicSlotDTO toDTO(TopicSlot topicSlot) {
        if (topicSlot == null) {
            return null;
        }

        TopicSlotDTO dto = new TopicSlotDTO();
        dto.setId(topicSlot.getId());
        dto.setTopicName(topicSlot.getTopicName());
        dto.setSlotName(topicSlot.getSlotName());
        dto.setSlotType(topicSlot.getSlotType());
        dto.setTimestampValue(topicSlot.getTimestampValue());
        dto.setTextValue(topicSlot.getTextValue());
        dto.setDoubleValue(topicSlot.getDoubleValue());
        dto.setLongValue(topicSlot.getLongValue());
        return dto;
    }
}