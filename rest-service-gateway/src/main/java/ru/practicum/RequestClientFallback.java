package ru.practicum;


import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.enumeration.ParticipationStatus;

import java.util.Collections;
import java.util.List;

@Slf4j
@Component
public class RequestClientFallback implements RequestClient {

    @Override
    public boolean existsByRequesterAndEventIdAndStatus(
            Long requesterId, Long eventId, ParticipationStatus status) {
        log.warn("Fallback triggered for existsByRequesterAndEventIdAndStatus. " +
                        "Requester: {}, Event: {}, Status: {}. Returning default: false",
                requesterId, eventId, status);
        return false;
    }

    @Override
    public long countByEventIdAndStatus(Long eventId, ParticipationStatus status) {
        log.warn("Fallback triggered for countByEventIdAndStatus. " +
                        "Event: {}, Status: {}. Returning default: 0",
                eventId, status);
        return 0L;
    }

    @Override
    public void bulkUpdateStatus(Long eventId, List<Long> requestIds, ParticipationStatus newStatus) {
        log.warn("Fallback triggered for bulkUpdateStatus. " +
                        "Event: {}, Request IDs count: {}, New status: {}. " +
                        "Operation skipped in fallback mode.",
                eventId, requestIds != null ? requestIds.size() : 0, newStatus);
    }

    @Override
    public List<ParticipationRequestDto> findAllByEventId(Long eventId) {
        log.warn("Fallback triggered for findAllByEventId. " +
                "Event: {}. Returning empty list.", eventId);
        return Collections.emptyList();
    }

    @Override
    public List<ParticipationRequestDto> findAllByEventIdAndStatus(Long eventId, ParticipationStatus status) {
        log.warn("Fallback triggered for findAllByEventIdAndStatus. " +
                        "Event: {}, Status: {}. Returning empty list.",
                eventId, status);
        return Collections.emptyList();
    }

    @Override
    public void rejectAllPendingRequests(Long eventId, ParticipationStatus rejectStatus) {
        log.warn("Fallback triggered for rejectAllPendingRequests. " +
                        "Event: {}, Reject status: {}. Operation skipped in fallback mode.",
                eventId, rejectStatus);
    }

    @Override
    public List<ParticipationRequestDto> findAllByEventIdAndIdIn(Long eventId, List<Long> requestIds) {
        log.warn("Fallback triggered for findAllByEventIdAndIdIn. " +
                        "Event: {}, Request IDs count: {}. Returning empty list.",
                eventId, requestIds != null ? requestIds.size() : 0);
        return Collections.emptyList();
    }
}