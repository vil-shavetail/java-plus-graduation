package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.proto.InteractionsCountRequestProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.SimilarEventsRequestProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;
import ru.practicum.model.Interaction;
import ru.practicum.model.Similarity;
import ru.practicum.repository.InteractionRepository;
import ru.practicum.repository.SimilarityRepository;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {
    private final SimilarityRepository similarityRepository;
    private final InteractionRepository interactionRepository;

    /**
     * Получает список похожих мероприятий для указанного события, исключая уже просмотренные пользователем.
     * @param request запрос с параметрами: ID события, ID пользователя, максимальное количество результатов
     * @return список рекомендованных мероприятий с оценками сходства
     */
    public List<RecommendedEventProto> getSimilarEvents(SimilarEventsRequestProto request) {
        log.info("Searching for similar events for event ID: {}", request.getEventId());
        long eventId = request.getEventId();
        long userId = request.getUserId();
        long maxResults = request.getMaxResults();
        if (maxResults <= 0) {
            maxResults = 10;
        }

        // Получаем список всех похожих событий
        List<Similarity> allSimilarities = similarityRepository
                .findAllByEvent1OrEvent2(eventId, eventId);

        // Получаем список событий, которые пользователь уже просматривал
        Set<Long> viewedEvents = interactionRepository.findByUserId(userId)
                .stream()
                .map(Interaction::getEventId)
                .collect(Collectors.toSet());

        // Фильтруем только непросмотренные события
        List<RecommendedEventProto> result = allSimilarities.stream()
                .filter(similarity -> {
                    Long otherEvent = getOtherEventId(similarity, eventId);
                    return otherEvent != null && !viewedEvents.contains(otherEvent);
                })
                .map(similarity -> {
                    Long otherEvent = getOtherEventId(similarity, eventId);
                    return RecommendedEventProto.newBuilder()
                            .setEventId(otherEvent)
                            .setScore(similarity.getSimilarityScore())
                            .build();
                })
                .sorted(Comparator.comparingDouble(RecommendedEventProto::getScore).reversed())
                .limit(maxResults)
                .toList();

        log.info("Found {} similar events", result.size());
        return result;
    }

    /**
     * Генерирует персонализированные рекомендации мероприятий для пользователя на основе его взаимодействий.
     * Учитывает сходство событий и взвешенные оценки взаимодействий пользователя.
     * @param request запрос с параметрами: ID пользователя, максимальное количество результатов
     * @return отсортированный список рекомендованных мероприятий с итоговыми взвешенными оценками
     */
    public List<RecommendedEventProto> generateUserRecommendations(UserPredictionsRequestProto request) {
        log.info("Generating recommendations for user ID: {}", request.getUserId());

        long userId = request.getUserId();
        long maxResults = request.getMaxResults(); // Всегда возвращает long (в proto3 0, если не задано)
        if (maxResults <= 0) {
            maxResults = 10;
        }

        // Получаем последние N взаимодействий пользователя
        List<Interaction> recentActions = interactionRepository.findByUserId(userId)
                .stream()
                .sorted(Comparator.comparing(Interaction::getTmsp).reversed())
                .limit(maxResults)
                .toList();

        if (recentActions.isEmpty()) {
            return Collections.emptyList();
        }

        // Формируем список идентификаторов событий, с которыми взаимодействовал пользователь
        Set<Long> userInteractedEvents = recentActions.stream()
                .map(Interaction::getEventId)
                .collect(Collectors.toSet());

        // Находим все события, похожие на те, с которыми взаимодействовал пользователь
        List<Similarity> similarEvents = similarityRepository
                .findAllBySourceEventIdInOrTargetEventIdIn(userInteractedEvents, userInteractedEvents);

        // Рассчитываем взвешенные оценки
        Map<Long, Double> weightedScores = new HashMap<>();
        for (Similarity similarity : similarEvents) {
            for (Long baseEventId : userInteractedEvents) {
                Long candidateEventId = getOtherEventId(similarity, baseEventId);

                if (candidateEventId == null || userInteractedEvents.contains(candidateEventId)) {
                    continue;
                }

                double userScore = calculateUserEventScore(baseEventId);
                double similarityScore = similarity.getSimilarityScore();
                weightedScores.put(candidateEventId,
                        weightedScores.getOrDefault(candidateEventId, 0.0) + (userScore * similarityScore));
            }
        }

        // Сортируем и формируем ответ
        return weightedScores.entrySet().stream()
                .sorted(Map.Entry.<Long, Double>comparingByValue().reversed())
                .limit(maxResults)
                .map(entry -> RecommendedEventProto.newBuilder()
                        .setEventId(entry.getKey())
                        .setScore(entry.getValue())
                        .build())
                .toList();
    }

    /**
     * Подсчитывает суммарное количество взаимодействий (с учётом рейтингов) для списка событий.
     * @param request запрос, содержащий список ID событий
     * @return список событий с суммарными рейтингами взаимодействий, отсортированный по убыванию
     */
    public List<RecommendedEventProto> countEventInteractions(InteractionsCountRequestProto request) {
        log.info("Counting interactions for events: {}", request.getEventIdList());

        Map<Long, Double> interactionCounts = interactionRepository.findByEventIdIn(request.getEventIdList())
                .stream()
                .collect(Collectors.groupingBy(
                        Interaction::getEventId,
                        Collectors.summingDouble(Interaction::getRating)
                ));

        return interactionCounts.entrySet().stream()
                .map(entry -> RecommendedEventProto.newBuilder()
                        .setEventId(entry.getKey())
                        .setScore(entry.getValue())
                        .build())
                .sorted(Comparator.comparingDouble(RecommendedEventProto::getScore).reversed())
                .collect(Collectors.toList());
    }

    /**
     * Возвращает идентификатор события, отличного от заданного в паре сходства.
     * Если ни одно из событий в паре не совпадает с baseEventId, возвращает null.
     * @param similarity объект сходства между двумя событиями
     * @param baseEventId ID исходного события
     * @return ID другого события в паре или null, если совпадение не найдено
     */
    private Long getOtherEventId(Similarity similarity, Long baseEventId) {
        if (similarity.getEvent1().equals(baseEventId)) {
            return similarity.getEvent2();
        } else if (similarity.getEvent2().equals(baseEventId)) {
            return similarity.getEvent1();
        }
        return null;
    }

    /**
     * Рассчитывает среднюю оценку (вес) взаимодействий пользователя с заданным мероприятием.
     * Если взаимодействий нет, возвращает значение по умолчанию — 1.0.
     * @param eventId ID мероприятия
     * @return средняя оценка взаимодействий или 1.0, если данных нет
     */
    private double calculateUserEventScore(Long eventId) {
        return interactionRepository.findByEventId(eventId)
                .stream()
                .mapToDouble(Interaction::getRating)
                .average()
                .orElse(1.0);
    }
}