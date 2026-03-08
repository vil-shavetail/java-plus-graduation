package ru.practicum;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import ru.practicum.dto.event.EventFullDto;

import java.util.Optional;

@FeignClient(name = "event-service", path = "/client/events")
public interface EventClient {
    @GetMapping("/{eventId}")
    Optional<EventFullDto> findById(@PathVariable Long eventId);

    @PutMapping("/{eventId}")
    EventFullDto save(@PathVariable Long eventId, @RequestBody EventFullDto event);
}
