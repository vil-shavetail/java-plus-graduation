package ru.practicum.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.Interaction;

import java.util.List;
import java.util.Optional;

public interface InteractionRepository extends JpaRepository<Interaction, Long> {
    @Query("SELECT i.rating FROM Interaction i WHERE i.userId = :userId AND i.eventId = :eventId")
    Double getRating(@Param("userId") long userId, @Param("eventId") long eventId);

    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO interaction (user_id, event_id, rating, tmsp)
            VALUES (:userId, :eventId, :rating, CURRENT_TIMESTAMP)
            ON CONFLICT (user_id, event_id)
            DO UPDATE SET rating = EXCLUDED.rating, tmsp = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    void updateRating(@Param("userId") long userId, @Param("eventId") long eventId, @Param("rating") double rating);

    List<Interaction> findByUserId(Long userId);

    List<Interaction> findByEventIdIn(List<Long> eventIds);

    List<Interaction> findByEventId(Long eventId);

    Optional<Interaction> findByUserIdAndEventId(Long userId, Long eventId);
}