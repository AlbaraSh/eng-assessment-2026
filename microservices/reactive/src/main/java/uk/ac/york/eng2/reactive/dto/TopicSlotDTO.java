package uk.ac.york.eng2.reactive.dto;

import io.micronaut.serde.annotation.Serdeable;

import java.time.Instant;

@Serdeable
public class TopicSlotDTO {
    private Long id;
    private String topicName;
    private String slotName;
    private String slotType;
    private Instant timestampValue;
    private String textValue;
    private Double doubleValue;
    private Long longValue;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTopicName() {
        return topicName;
    }

    public void setTopicName(String topicName) {
        this.topicName = topicName;
    }

    public String getSlotName() {
        return slotName;
    }

    public void setSlotName(String slotName) {
        this.slotName = slotName;
    }

    public String getSlotType() {
        return slotType;
    }

    public void setSlotType(String slotType) {
        this.slotType = slotType;
    }

    public Instant getTimestampValue() {
        return timestampValue;
    }

    public void setTimestampValue(Instant timestampValue) {
        this.timestampValue = timestampValue;
    }

    public String getTextValue() {
        return textValue;
    }

    public void setTextValue(String textValue) {
        this.textValue = textValue;
    }

    public Double getDoubleValue() {
        return doubleValue;
    }

    public void setDoubleValue(Double doubleValue) {
        this.doubleValue = doubleValue;
    }

    public Long getLongValue() {
        return longValue;
    }

    public void setLongValue(Long longValue) {
        this.longValue = longValue;
    }
}