package ru.practicum.service;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.ewm.stats.avro.ActionTypeAvro;
import ru.practicum.ewm.stats.avro.UserActionAvro;
import ru.practicum.model.Interaction;
import ru.practicum.repository.InteractionRepository;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Slf4j
@Service
@AllArgsConstructor
public class InteractionService {
    private final InteractionRepository interactionRepository;

    /**
     * Обрабатывает действие пользователя и сохраняет/обновляет запись о взаимодействии.
     * Если взаимодействие уже существует и его рейтинг ниже нового — обновляет.
     * Иначе создаёт новую запись.
     */
    @Transactional
    public void processUserAction(UserActionAvro userActionAvro) {
        if (userActionAvro == null) {
            log.warn("Received null user action request");
            return;
        }
        long userId = userActionAvro.getUserId();
        long eventId = userActionAvro.getEventId();
        double newScore = calcInteractionScore(userActionAvro.getActionType());
        log.debug("Processing user action for user {}: {}", userId, userActionAvro.getActionType());

        // Определяем локальное время из timestamp Avro (UTC)
        LocalDateTime interactAt = LocalDateTime.ofInstant(
                userActionAvro.getTimestamp(), ZoneId.of("UTC"));

        interactionRepository.findByUserIdAndEventId(userId, eventId)
                .ifPresentOrElse(
                        existingAction -> {
                            if (existingAction.getRating() < newScore) {
                                updateInteractionRecord(existingAction, newScore, interactAt);
                            } else {
                                log.debug("User action for user {} and event {} not updated: current score {} >= {}",
                                        userId, eventId, existingAction.getRating(), newScore);
                            }
                        },
                        () -> {
                            createNewInteractionRecord(userId, eventId, newScore, interactAt);
                            log.info("Created new user interaction for user {} with event {}", userId, eventId);
                        }
                );
    }

    /**
     * Обновляет существующую запись о взаимодействии пользователя с мероприятием.
     * Устанавливает новый рейтинг и обновляет временную метку.
     */
    private void updateInteractionRecord(Interaction existingAction, double newScore, LocalDateTime interactAt) {
        existingAction.setRating(newScore);
        existingAction.setTmsp(interactAt);
        interactionRepository.save(existingAction);
        log.info("Updated user interaction for user {} with event {}: score {}",
                existingAction.getUserId(), existingAction.getEventId(), newScore);
    }

    /**
     * Создаёт новую запись о взаимодействии пользователя с мероприятием в базе данных.
     */
    private void createNewInteractionRecord(long userId, long eventId, double newScore, LocalDateTime interactAt) {
        Interaction newUserAction = Interaction.builder()
                .userId(userId)
                .eventId(eventId)
                .rating(newScore)
                .tmsp(interactAt)
                .build();
        interactionRepository.save(newUserAction);
    }

    /**
     * Рассчитывает вес взаимодействия на основе типа действия пользователя.
     * Возвращает числовое значение от 0.0 до 1.0 в зависимости от типа действия.
     * Для неизвестных типов действий возвращает 0.0 и логирует предупреждение.
     */
    private double calcInteractionScore(ActionTypeAvro type) {
        return switch (type) {
            case VIEW -> 0.4;
            case REGISTER -> 0.8;
            case LIKE -> 1.0;
            default -> {
                log.warn("Unknown action type: {}", type);
                yield 0.0; // Default value
            }
        };
    }
}