package ru.practicum.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Событие (рекомендации для пользователя)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventRecommendationDto {
    /**
     * Инициатор события
     */
    private long eventId;

    /**
     * Рейтинг события
     */
    private double score;
}
