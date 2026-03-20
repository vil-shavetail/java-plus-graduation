package ru.practicum.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.querydsl.QuerydslPredicateExecutor;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import ru.practicum.model.Event;

/**
 * Репозиторий для работы с событиями
 */
@Repository
public interface EventRepository extends JpaRepository<Event, Long>, QuerydslPredicateExecutor<Event> {
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO LIKES_EVENTS (USER_ID, EVENT_ID) values (:userId, :eventId)", nativeQuery = true)
    void addLike(Long userId, Long eventId);

    @Query(value = "SELECT EXISTS (" +
            "SELECT * FROM LIKES_EVENTS WHERE USER_ID = :userId AND EVENT_ID = :eventId)", nativeQuery = true)
    boolean checkLikeExisting(Long userId, Long eventId);

    @Query(value = "SELECT COUNT(*) FROM LIKES_EVENTS WHERE EVENT_ID = :eventId", nativeQuery = true)
    long countLikesByEventId(Long eventId);
}