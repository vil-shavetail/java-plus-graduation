package ru.practicum.controller;


import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.enumeration.ParticipationStatus;
import ru.practicum.service.ParticipationRequestService;

import java.util.List;

@RestController
@RequestMapping("/client/requests")
@RequiredArgsConstructor
public class RequestClientController {

    private final ParticipationRequestService requestService;

    @GetMapping("/exists")
    public ResponseEntity<Boolean> existsByRequesterAndEventIdAndStatus(
            @RequestParam("requesterId") Long requesterId,
            @RequestParam("eventId") Long eventId,
            @RequestParam("status") ParticipationStatus status) {
        boolean exists = requestService.existsByRequesterAndEventIdAndStatus(requesterId, eventId, status);
        return ResponseEntity.ok(exists);
    }

    @GetMapping("/count")
    public ResponseEntity<Long> countByEventIdAndStatus(
            @RequestParam("eventId") Long eventId,
            @RequestParam("status") ParticipationStatus status) {
        long count = requestService.countByEventIdAndStatus(eventId, status);
        return ResponseEntity.ok(count);
    }

    @PostMapping("/bulk-update-status")
    public ResponseEntity<Void> bulkUpdateStatus(
            @RequestParam("eventId") Long eventId,
            @RequestBody List<Long> requestIds,
            @RequestParam("newStatus") ParticipationStatus newStatus) {
        requestService.bulkUpdateStatus(eventId, requestIds, newStatus);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{eventId}")
    public ResponseEntity<List<ParticipationRequestDto>> findAllByEventId(
            @PathVariable("eventId") Long eventId) {
        List<ParticipationRequestDto> requests = requestService.findAllByEventId(eventId);
        return ResponseEntity.ok(requests);
    }

    @GetMapping("/event/{eventId}/status/{status}")
    public ResponseEntity<List<ParticipationRequestDto>> findAllByEventIdAndStatus(
            @PathVariable("eventId") Long eventId,
            @PathVariable("status") ParticipationStatus status) {
        List<ParticipationRequestDto> requests = requestService.findAllByEventIdAndStatus(eventId, status);
        return ResponseEntity.ok(requests);
    }

    @PostMapping("/reject")
    public ResponseEntity<Void> rejectAllPendingRequests(
            @RequestParam("eventId") Long eventId,
            @RequestParam("rejectStatus") ParticipationStatus rejectStatus) {
        requestService.rejectAllPendingRequests(eventId, rejectStatus);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/requests/by-event-and-ids")
    public ResponseEntity<List<ParticipationRequestDto>> findAllByEventIdAndIdIn(
            @RequestParam("eventId") Long eventId,
            @RequestBody List<Long> requestIds) {
        List<ParticipationRequestDto> requests = requestService.findAllByEventIdAndIdIn(eventId, requestIds);
        return ResponseEntity.ok(requests);
    }
}