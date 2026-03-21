package ru.practicum.kafka.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.service.SimilarityService;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventSimilarityConsumer {
    private final SimilarityService similarityService;

    @KafkaListener(
            topics = "${analyzer.kafka.consumer.topic.events}",
            containerFactory = "similarityListenerContainerFactory"
    )
    public void processEventSimilarity(EventSimilarityAvro eventSimilarityAvro) {
        log.info("Event similarity received: {}", eventSimilarityAvro);
        similarityService.processEventSimilarity(eventSimilarityAvro);
    }
}