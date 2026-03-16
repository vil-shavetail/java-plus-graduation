package ru.practicum.kafka.producer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.UserActionAvro;

@Slf4j
@Service
public class UserActionProducer {

    private final KafkaTemplate<String, UserActionAvro> kafkaTemplate;
    private final String topicName;

    public UserActionProducer(KafkaTemplate<String, UserActionAvro> kafkaTemplate,
                              @Value("${kafka.topic}") String topicName) {
        this.kafkaTemplate = kafkaTemplate;
        this.topicName = topicName;
    }

    public void sendUserAction(UserActionAvro userAction) {
        kafkaTemplate.send(topicName, userAction);
        log.info("Sent user action to Kafka: userId={}, eventId={}, actionType={}",
                userAction.getUserId(), userAction.getEventId(), userAction.getActionType());
    }
}