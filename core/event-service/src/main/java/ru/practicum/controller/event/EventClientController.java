package ru.practicum.controller.event;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.service.EventService;

import java.util.Optional;

@RestController
@RequestMapping("/client/events")
@RequiredArgsConstructor
public class EventClientController {

    private final EventService eventService;

    @GetMapping("/{eventId}")
    public ResponseEntity<Optional<EventFullDto>> findById(@PathVariable Long eventId) {
        Optional<EventFullDto> event = eventService.findById(eventId);
        return ResponseEntity.ok(event);
    }

    @PutMapping("/{eventId}")
    public ResponseEntity<EventFullDto> save(
            @PathVariable Long eventId,
            @RequestBody EventFullDto event) {
        EventFullDto savedEvent = eventService.save(eventId, event);
        return ResponseEntity.ok(savedEvent);
    }
}