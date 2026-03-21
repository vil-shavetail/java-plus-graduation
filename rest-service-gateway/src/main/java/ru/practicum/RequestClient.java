package ru.practicum;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.context.annotation.Primary;
import org.springframework.web.bind.annotation.*;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.enumeration.ParticipationStatus;

import java.util.List;

@Primary
@FeignClient(name = "request-service", path = "/client/request", fallback = RequestClientFallback.class)
public interface RequestClient {
    @GetMapping("/exists?requesterId={requesterId}&eventId={eventId}&status={status}")
    boolean existsByRequesterAndEventIdAndStatus(
            @PathVariable("requesterId") Long requesterId,
            @PathVariable("eventId") Long eventId,
            @PathVariable("status") ParticipationStatus status
    );

    @GetMapping("/count?eventId={eventId}&status={status}")
    long countByEventIdAndStatus(
            @PathVariable("eventId") Long eventId,
            @PathVariable("status") ParticipationStatus status
    );

    @PostMapping("/bulk-update-status")
    void bulkUpdateStatus(
            @RequestParam("eventId") Long eventId,
            @RequestBody List<Long> requestIds,
            @RequestParam("newStatus") ParticipationStatus newStatus
    );

    @GetMapping("/{eventId}")
    List<ParticipationRequestDto> findAllByEventId(
            @PathVariable("eventId") Long eventId
    );

    @GetMapping("/event/{eventId}/status/{status}")
    List<ParticipationRequestDto> findAllByEventIdAndStatus(
            @PathVariable("eventId") Long eventId,
            @PathVariable("status") ParticipationStatus status
    );

    @PostMapping("/reject")
    void rejectAllPendingRequests(
            @RequestParam("eventId") Long eventId,
            @RequestParam("rejectStatus") ParticipationStatus rejectStatus
    );

    @PostMapping("/requests/by-event-and-ids")
    List<ParticipationRequestDto> findAllByEventIdAndIdIn(
            @RequestParam("eventId") Long eventId,
            @RequestBody List<Long> requestIds
    );
}
