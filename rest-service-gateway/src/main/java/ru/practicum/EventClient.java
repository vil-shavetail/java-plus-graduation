package ru.practicum;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import ru.practicum.dto.event.EventFullDto;


@FeignClient(name = "event-service", path = "/client/event")
public interface EventClient {
    @GetMapping("/{eventId}")
    EventFullDto findById(@PathVariable Long eventId);

    @PutMapping("/{eventId}/increment-confirmed-requests")
    Long incrementConfirmedRequests(@PathVariable Long eventId);
}
