package ru.practicum.controller.event;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.event.EventSFRDto;
import ru.practicum.service.EventService;


@RestController
@RequestMapping("/client/event")
@RequiredArgsConstructor
public class EventClientController {

    private final EventService eventService;

    @GetMapping("/{eventId}")
    public ResponseEntity<EventSFRDto> findById(@PathVariable Long eventId) {
        EventSFRDto event = eventService.findById(eventId);
        return ResponseEntity.ok(event);
    }

    @PutMapping("/{eventId}/increment-confirmed-requests")
    public ResponseEntity<Void> incrementConfirmedRequests(@PathVariable Long eventId) {
        eventService.incrementConfirmedRequests(eventId);
        return ResponseEntity.noContent().build();
    }
}