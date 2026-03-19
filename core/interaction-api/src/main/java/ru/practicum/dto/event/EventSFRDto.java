package ru.practicum.dto.event;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Событие (информация для работы с заявками)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EventSFRDto {
    /**
     * Идентификатор
     */
    private Long id;

    /**
     * Количество одобренных заявок на участие в данном событии
     */
    private Long confirmedRequests;

    /**
     * Инициатор события
     */
    @NotNull
    private Long initiator;

    /**
     * Ограничение на количество участников. 0 = без ограничения
     */
    private Integer participantLimit;

    /**
     * Дата и время публикации события
     */
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime publishedOn;

    /**
     * Нужна ли пре-модерация заявок на участие
     */
    private Boolean requestModeration;

    private Double rating;
}