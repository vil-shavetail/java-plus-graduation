package ru.practicum.controller.event;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import ru.practicum.AnalyzerClient;
import ru.practicum.CollectorClient;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.event.EventRecommendationDto;
import ru.practicum.dto.event.EventShortDto;
import ru.practicum.enumeration.EventSort;
import ru.practicum.ewm.stats.proto.ActionTypeProto;
import ru.practicum.ewm.stats.proto.RecommendedEventProto;
import ru.practicum.ewm.stats.proto.UserActionProto;
import ru.practicum.ewm.stats.proto.UserPredictionsRequestProto;
import ru.practicum.exception.BadRequestException;
import ru.practicum.service.EventService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;


/**
 * Публичный API для работы с событиями
 */
@RestController
@RequestMapping("/events")
@RequiredArgsConstructor
@Slf4j
@Validated
public class PublicEventController {
    public static final String X_EWM_USER_ID = "X-EWM-USER-ID";
    private final EventService eventService;
    private final AnalyzerClient analyzer;
    private final CollectorClient collector;

    /**
     * Получение событий с возможностью фильтрации
     *
     * @param text текст для поиска в содержимом аннотации и подробном описании события
     * @param categories список идентификаторов категорий
     * @param paid поиск только платных/бесплатных событий
     * @param rangeStart дата и время не раньше которых должно произойти событие
     * @param rangeEnd дата и время не позже которых должно произойти событие
     * @param onlyAvailable только события у которых не исчерпан лимит запросов на участие
     * @param sort вариант сортировки: по дате события или по количеству просмотров
     * @param from количество событий, которые нужно пропустить для формирования текущего набора
     * @param size количество событий в наборе
     * @param request HTTP запрос (для получения IP и URI для статистики)
     * @return список событий
     */
    @GetMapping
    public List<EventShortDto> getEvents(
            @RequestParam(required = false) String text,
            @RequestParam(required = false) List<Long> categories,
            @RequestParam(required = false) Boolean paid,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss") LocalDateTime rangeEnd,
            @RequestParam(defaultValue = "false") Boolean onlyAvailable,
            @RequestParam(required = false) EventSort sort,
            @RequestParam(defaultValue = "0") @PositiveOrZero Integer from,
            @RequestParam(defaultValue = "10") @Positive Integer size,
            HttpServletRequest request) {

        log.info("GET /events: text={}, categories={}, paid={}, rangeStart={}, rangeEnd={}, " +
                "onlyAvailable={}, sort={}, from={}, size={}",
                text, categories, paid, rangeStart, rangeEnd, onlyAvailable, sort, from, size);

        return eventService.getPublicEvents(text, categories, paid, rangeStart, rangeEnd,
                                             onlyAvailable, sort, from, size, request);
    }

    /**
     * Получение подробной информации об опубликованном событии по его идентификатору
     *
     * @param id id события
     * @param userId id пользователя
     * @return подробная информация о событии
     */
    @GetMapping("/{id}")
    public EventFullDto getEventById(
            @PathVariable @Min(1) Long id,
            @RequestHeader(X_EWM_USER_ID) Long userId) {

        log.info("GET /events/{}: id={}, userId={}", id, id, userId);
        return eventService.getPublicEventById(id, userId);
    }

    @GetMapping("/recommendations")
    public List<EventRecommendationDto> getEventRecommendations(
            @RequestHeader(X_EWM_USER_ID) long userId,
            @RequestParam(defaultValue = "10") @Positive Integer size) {

        log.info("GET /events/recommendations: userId={}, size={}", userId, size);

        UserPredictionsRequestProto request = UserPredictionsRequestProto.newBuilder()
                .setUserId(userId)
                .setMaxResults(size)
                .build();

        List<RecommendedEventProto> recommendations = analyzer.getRecommendedEventsForUser(request);

        return recommendations.stream()
                .map(recommendedEvent -> EventRecommendationDto.builder()
                        .eventId(recommendedEvent.getEventId())
                        .score(recommendedEvent.getScore())
                        .build())
                .collect(Collectors.toList());
    }

    @PutMapping("/{eventId}/like")
    public void likeEvent(
            @PathVariable @Min(1) Long eventId,
            @RequestHeader(X_EWM_USER_ID) Long userId) {

        log.info("PUT /events/{}/like: eventId={}, userId={}", eventId, eventId, userId);

        if (!eventService.hasUserVisitedEvent(userId, eventId)) {
            throw new BadRequestException("Пользователь не посещал это мероприятие");
        }

        // Отправляем информацию об отправке лайка
        long seconds = Instant.now().getEpochSecond();
        int nanos = Instant.now().getNano();
        UserActionProto actionProto = UserActionProto.newBuilder()
                .setUserId(userId)
                .setEventId(eventId)
                .setActionType(ActionTypeProto.ACTION_LIKE)
                .setTimestamp(
                        com.google.protobuf.Timestamp.newBuilder()
                                .setSeconds(seconds)
                                .setNanos(nanos)
                )
                .build();

        collector.sendUserAction(actionProto);
    }
}