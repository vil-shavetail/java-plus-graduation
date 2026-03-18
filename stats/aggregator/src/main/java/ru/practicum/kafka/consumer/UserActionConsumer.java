package ru.practicum.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.service.AggregatorService;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionConsumer {
    private final AggregatorService aggregatorService;

    @KafkaListener(topics = "${aggregator.kafka.consumer.topic.actions}")
    public void consumeUserAction(UserActionAvro userAction) {
        log.info("Received user action: userId={}, eventId={}, actionType={}",
                userAction.getUserId(), userAction.getEventId(), userAction.getActionType());
        aggregatorService.processUserAction(userAction);
    }
}