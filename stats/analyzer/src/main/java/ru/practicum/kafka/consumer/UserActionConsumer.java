package ru.practicum.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.service.InteractionService;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserActionConsumer {
    private final InteractionService interactionService;

    @KafkaListener(
            topics = "${analyzer.kafka.consumer.topic.actions}",
            containerFactory = "userActionListenerContainerFactory"
    )
    public void processUserAction(UserActionAvro actionAvro) {
        log.info("Processing user action: {}", actionAvro);
        interactionService.processUserAction(actionAvro);
    }
}