package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.model.Similarity;
import ru.practicum.repository.SimilarityRepository;

import java.time.ZoneOffset;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class SimilarityService {
    private final SimilarityRepository similarityRepository;

    /**
     * Обрабатывает событие схожести мероприятий, полученное из Kafka.
     * Создаёт или обновляет запись о схожести в базе данных.
     *
     * @param eventSimilarityAvro объект события схожести из Kafka
     */
    public void processEventSimilarity(EventSimilarityAvro eventSimilarityAvro) {
        log.info("Processing event similarity: {}", eventSimilarityAvro);
        Similarity eventSimilarity = Similarity.builder()
                .event1(eventSimilarityAvro.getEventA())
                .event2(eventSimilarityAvro.getEventB())
                .similarityScore(eventSimilarityAvro.getScore())
                .tmsp(eventSimilarityAvro.getTimestamp()
                        .atZone(ZoneOffset.UTC)
                        .toLocalDateTime())
                .build();

        Optional<Similarity> existing = similarityRepository.findByEvent1AndEvent2(
                        eventSimilarity.getEvent1(),
                        eventSimilarity.getEvent2()
                );

        if (existing.isPresent()) {
            log.debug("Updating existing similarity record for events {} and {}",
                    eventSimilarity.getEvent1(), eventSimilarity.getEvent2());
            Similarity updated = existing.get();
            updated.setSimilarityScore(eventSimilarity.getSimilarityScore());
            updated.setTmsp(eventSimilarity.getTmsp());
            similarityRepository.save(updated);
        } else {
            log.debug("Creating new similarity record for events {} and {}",
                    eventSimilarity.getEvent1(), eventSimilarity.getEvent2());
            similarityRepository.save(eventSimilarity);
        }
    }
}