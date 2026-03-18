package ru.practicum.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.EventSimilarityAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.kafka.config.KafkaConfig;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class AggregatorService {
    private final KafkaTemplate<String, EventSimilarityAvro> kafkaTemplate;
    private final KafkaConfig kafkaConfig;

    // Веса действий: eventId → userId → maxWeight
    private final Map<Long, Map<Long, Double>> userEventWeights = new ConcurrentHashMap<>();

    // Суммы весов по мероприятиям: eventId → sumOfWeights
    private final Map<Long, Double> eventWeightSums = new ConcurrentHashMap<>();

    // Суммы минимальных весов: (eventA, eventB) → sumOfMinWeights
    // Ключи упорядочены: eventA < eventB
    private final Map<Long, Map<Long, Double>> minWeightsSums = new ConcurrentHashMap<>();

    // Константы весов действий
    private static final double VIEW_WEIGHT = 0.4;
    private static final double REGISTER_WEIGHT = 0.8;
    private static final double LIKE_WEIGHT = 1.0;

    /**
     * Возвращает вес действия пользователя в зависимости от его типа.
     * Вес используется для количественной оценки значимости действия при расчёте сходства мероприятий.
     *
     * @param actionType тип действия пользователя (VIEW, REGISTER, LIKE)
     * @return числовое значение веса действия
     */
    private double getActionWeight(ActionTypeAvro actionType) {
        return switch (actionType) {
            case VIEW -> VIEW_WEIGHT;
            case REGISTER -> REGISTER_WEIGHT;
            case LIKE -> LIKE_WEIGHT;
        };
    }

    /**
     * Создаёт объект EventSimilarityAvro, представляющий сходство между двумя мероприятиями.
     * Гарантирует упорядоченность идентификаторов мероприятий: eventA всегда меньше eventB.
     *
     * @param a идентификатор первого мероприятия
     * @param b идентификатор второго мероприятия
     * @param score рассчитанный коэффициент сходства (от 0 до 1)
     * @return готовый объект EventSimilarityAvro для отправки в Kafka
     */
    private EventSimilarityAvro createSimilarityAvro(long a, long b, double score) {
        return EventSimilarityAvro.newBuilder()
                .setEventA(Math.min(a, b))
                .setEventB(Math.max(a, b))
                .setScore(score)
                .setTimestamp(Instant.now())
                .build();
    }

    /**
     * Обновляет сумму минимальных весов для пары мероприятий и рассчитывает новое косинусное сходство.
     * Выполняет следующие шаги:
     * 1. Вычисляет изменение суммы минимальных весов (deltaMin)
     * 2. Обновляет накопленную сумму минимальных весов
     * 3. Рассчитывает косинусное сходство по формуле: sumOfMinWeights / (normA * normB)
     *
     * @param eventA идентификатор первого мероприятия (должен быть меньше eventB)
     * @param eventB идентификатор второго мероприятия (должен быть больше eventA)
     * @param oldWA старый вес пользователя для мероприятия A
     * @param newWA новый вес пользователя для мероприятия A
     * @param weightB вес пользователя для мероприятия B
     * @return Optional с объектом сходства, если расчёт успешен; пустой Optional, если норма одного из мероприятий равна 0
     */
    private Optional<EventSimilarityAvro> updateSimilarityPair(long eventA, long eventB,
                                                               double oldWA, double newWA, double weightB) {
        // Вычисляем старые и новые минимальные веса для пары
        double oldMin = Math.min(oldWA, weightB);
        double newMin = Math.min(newWA, weightB);
        double deltaMin = newMin - oldMin;

        // Обновляем сумму минимальных весов в хранилище
        double currentMinSum = updateMinWeightsSum(eventA, eventB, deltaMin);

        // Рассчитываем нормы векторов весов для обоих мероприятий
        double normA = Math.sqrt(eventWeightSums.getOrDefault(eventA, 0.0));
        double normB = Math.sqrt(eventWeightSums.getOrDefault(eventB, 0.0));

        // Если норма хотя бы одного мероприятия равна 0, сходство не может быть рассчитано
        if (normA == 0 || normB == 0) return Optional.empty();

        // Косинусное сходство: сумма минимальных весов / (норма A * норма B)
        double score = currentMinSum / (normA * normB);
        return Optional.of(createSimilarityAvro(eventA, eventB, score));
    }

    /**
     * Обновляет сумму минимальных весов для пары мероприятий с учётом дельты изменения.
     * Использует ConcurrentHashMap для потокобезопасного обновления данных.
     *
     * @param a идентификатор первого мероприятия
     * @param b идентификатор второго мероприятия
     * @param delta изменение суммы минимальных весов (может быть положительным или отрицательным)
     * @return новое значение суммы минимальных весов для данной пары мероприятий
     */
    private double updateMinWeightsSum(long a, long b, double delta) {
        long first = Math.min(a, b);
        long second = Math.max(a, b);
        return minWeightsSums.computeIfAbsent(first, k -> new ConcurrentHashMap<>())
                .merge(second, delta, Double::sum);
    }

    /**
     * Отправляет список объектов сходства в Kafka для дальнейшей обработки.
     * Используется для передачи рассчитанных коэффициентов сходства между парами мероприятий.
     *
     * @param list список объектов EventSimilarityAvro, содержащих информацию о сходстве
     */
    private void sendSimilarity(List<EventSimilarityAvro> list) {
        list.forEach(sim -> kafkaTemplate.send(kafkaConfig.getEvents(), sim));
    }

    /**
     * Основной метод обработки действия пользователя. Выполняет следующие задачи:
     * 1. Определяет вес действия по его типу
     * 2. Проверяет, увеличился ли вес действия (если нет — игнорирует)
     * 3. Обновляет локальное состояние (веса и суммы)
     * 4. Пересчитывает сходство с другими мероприятиями, с которыми взаимодействовал пользователь
     * 5. Отправляет обновлённые коэффициенты сходства в Kafka
     *
     * @param action объект UserActionAvro, содержащий информацию о действии пользователя
     */
    public void processUserAction(UserActionAvro action) {
        double newWeight = getActionWeight(action.getActionType());
        long userId = action.getUserId();
        long eventId = action.getEventId();

        // Получаем старый вес действия пользователя для этого мероприятия
        Map<Long, Double> userWeights = userEventWeights.computeIfAbsent(eventId, k -> new ConcurrentHashMap<>());

        // Если новый вес не превышает старый, игнорируем действие
        Double oldWeight = userWeights.getOrDefault(userId, 0.0);
        if (newWeight <= oldWeight) {
            log.debug("Вес не увеличился (old: {}, new: {}). Игнорируем.", oldWeight, newWeight);
            return;
        }

        // Обновляем локальное состояние:
        userWeights.put(userId, newWeight);
        double delta = newWeight - oldWeight;
        eventWeightSums.merge(eventId, delta, Double::sum);

        // Список для хранения обновлённых коэффициентов сходства
        List<EventSimilarityAvro> similarities = new ArrayList<>();

        // Перебираем все мероприятия, с которыми взаимодействовали пользователи
        for (Map.Entry<Long, Map<Long, Double>> entry : userEventWeights.entrySet()) {
            long otherEventId = entry.getKey();
            if (otherEventId == eventId) continue;

            // Если пользователь взаимодействовал и с 'eventId', и с 'otherEventId'
            Double weightInOtherEvent = entry.getValue().get(userId);
            if (weightInOtherEvent != null) {
                updateSimilarityPair(eventId, otherEventId, oldWeight, newWeight, weightInOtherEvent)
                        .ifPresent(similarities::add);
            }
        }
        sendSimilarity(similarities);
    }
}