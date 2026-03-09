package ru.practicum.service;

import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import ru.practicum.EventClient;
import ru.practicum.UserClient;
import ru.practicum.dto.event.EventFullDto;
import ru.practicum.dto.request.ParticipationRequestDto;
import ru.practicum.dto.user.UserDto;
import ru.practicum.enumeration.ParticipationStatus;
import ru.practicum.exception.ConflictException;
import ru.practicum.exception.NotFoundException;
import ru.practicum.mapper.ParticipationRequestMapper;
import ru.practicum.model.ParticipationRequest;
import ru.practicum.repository.ParticipationRequestRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ParticipationRequestService {

    private final ParticipationRequestRepository requestRepository;
    private final UserClient userClient;
    private final EventClient eventClient;

    @Transactional
    public ParticipationRequestDto addRequest(Long userId, Long eventId) {
        log.info("Creating a request for event: {} by user: {}", eventId, userId);
        UserDto requester = userClient.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User with ID {} not found", userId);
                    return new NotFoundException("User with id: " + userId + "was not found");
                });
        EventFullDto event = eventClient.findById(eventId)
                .orElseThrow(() -> {
                    log.warn("Event with ID {} not found", eventId);
                    return new NotFoundException("Event with id: " + eventId + "was not found");
                });
        if (event.getInitiator().equals(userId)) {
            throw new ConflictException("User cannot request participation in their own event");
        }
        if (event.getPublishedOn() == null || event.getPublishedOn().toString().trim().isEmpty()) {
            throw new ConflictException("Cannot participate in an unpublished event");
        }
        Integer participantLimit = event.getParticipantLimit();
        Integer confirmedRequests = event.getConfirmedRequests();
        if (participantLimit != 0 && confirmedRequests >= participantLimit) {
            throw new ConflictException("Participation request limit reached for event id=" + eventId);
        }
        if (requestRepository.findByRequesterAndEventId(userId, eventId).isPresent()) {
            throw new ConflictException("Duplicate participation request");
        }
        ParticipationRequest request = new ParticipationRequest();
        request.setRequester(requester.getId());
        request.setEventId(event.getId());
        if (Boolean.FALSE.equals(event.getRequestModeration()) || participantLimit == 0) {
            request.setStatus(ParticipationStatus.CONFIRMED);
        } else {
            request.setStatus(ParticipationStatus.PENDING);
        }
        request.setCreated(LocalDateTime.now());
        ParticipationRequest savedRequest = requestRepository.save(request);
        if (savedRequest.getStatus() == ParticipationStatus.CONFIRMED) {
            event.setConfirmedRequests(confirmedRequests + 1);
            eventClient.save(event.getId(), event);
            log.info("Updated confirmedRequests for event {} to {}", eventId, event.getConfirmedRequests());
        }
        log.info("The request was successfully created: {}", savedRequest);
        return ParticipationRequestMapper.INSTANCE.toDto(savedRequest);
    }

    @Transactional
    public ParticipationRequestDto cancelRequest(Long userId, Long requestId) {
        log.info("Creating a cancellation request: {} for event by user: {}", requestId, userId);
        ParticipationRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new RuntimeException("Request with id: " + requestId + " was not found"));
        UserDto requester = userClient.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User with ID {} not found", userId);
                    return new NotFoundException("User with id: " + userId + "was not found");
                });
        if (!request.getRequester().equals(requester.getId())) {
            throw new NotFoundException("Request with id=" + requestId + " does not belong to user " + userId);
        }
        request.setStatus(ParticipationStatus.CANCELED);
        ParticipationRequest savedRequest = requestRepository.save(request);
        log.info("The request: {} was successfully rejected", savedRequest);
        return ParticipationRequestMapper.INSTANCE.toDto(savedRequest);
    }

    public List<ParticipationRequestDto> getUserRequests(Long userId) {
        log.info("Searching event requests for the user with id: {}", userId);
        UserDto existingUser = userClient.findById(userId)
                .orElseThrow(() -> {
                    log.warn("User with ID {} not found", userId);
                    return new NotFoundException("User with id: " + userId + "was not found");
                });
        List<ParticipationRequestDto> requests = requestRepository.findAllByRequester(existingUser.getId())
                .stream()
                .map(ParticipationRequestMapper.INSTANCE::toDto)
                .toList();
        log.info("Event requests was found: {}", requests.size());
        return requests;
    }

    public boolean existsByRequesterAndEventIdAndStatus(Long requesterId, Long eventId, ParticipationStatus status) {
        return requestRepository.existsByRequesterAndEventIdAndStatus(requesterId, eventId, status);
    }

    public long countByEventIdAndStatus(Long eventId, ParticipationStatus status) {
        return requestRepository.countByEventIdAndStatus(eventId, status);
    }

    @Transactional
    public void bulkUpdateStatus(Long eventId, List<Long> requestIds, ParticipationStatus newStatus) {
        if (requestIds.isEmpty()) {
            return;
        }
        requestRepository.bulkUpdateStatus(eventId, requestIds, newStatus);
    }

    public List<ParticipationRequestDto> findAllByEventId(Long eventId) {
        return requestRepository.findAllByEventId(eventId).stream()
                .map(ParticipationRequestMapper.INSTANCE::toDto)
                .collect(Collectors.toList());
    }

    public List<ParticipationRequestDto> findAllByEventIdAndStatus(Long eventId, ParticipationStatus status) {
        return requestRepository.findAllByEventIdAndStatus(eventId, status).stream()
                .map(ParticipationRequestMapper.INSTANCE::toDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public void rejectAllPendingRequests(Long eventId, ParticipationStatus rejectStatus) {
        requestRepository.rejectAllPendingRequests(eventId, rejectStatus);
    }

    public List<ParticipationRequestDto> findAllByEventIdAndIdIn(Long eventId, List<Long> requestIds) {
        return requestRepository.findAllByEventIdAndIdIn(eventId, requestIds).stream()
                .map(ParticipationRequestMapper.INSTANCE::toDto)
                .collect(Collectors.toList());
    }
}
