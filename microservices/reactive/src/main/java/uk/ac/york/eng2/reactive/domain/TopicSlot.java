package uk.ac.york.eng2.reactive.domain;

import io.micronaut.serde.annotation.Serdeable;
import jakarta.persistence.*;

import java.time.Instant;

@Serdeable
@Entity
@Table(name = "topic_slot")
public class TopicSlot {

    @Id
    @GeneratedValue
    private Long id;

    @Column(name = "topic_name")
    private String topicName;

    @Column(name = "slot_name")
    private String slotName;

    @Column(name = "slot_type")
    private String slotType;

    @Column(name = "timestamp_value")
    private Instant timestampValue;

    @Column(name = "text_value")
    private String textValue;

    @Column(name = "double_value")
    private Double doubleValue;

    @Column(name = "long_value")
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