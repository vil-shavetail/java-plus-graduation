package ru.practicum.repository;

import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import ru.practicum.model.Similarity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface SimilarityRepository extends JpaRepository<Similarity, Long> {
    @Modifying
    @Transactional
    @Query(value = """
            INSERT INTO similarity (event1, event2, similarity_score, tmsp)
            VALUES (:event1, :event2, :score, CURRENT_TIMESTAMP)
            ON CONFLICT (event1, event2)
            DO UPDATE SET similarity_score = EXCLUDED.similarity_score, tmsp = CURRENT_TIMESTAMP
            """, nativeQuery = true)
    void updateScore(@Param("event1") long event1, @Param("event2") long event2, @Param("score") double score);

    @Query("""
            SELECT es
            FROM Similarity es
            WHERE es.event1 = :event1 OR es.event2 = :event2
            """)
    List<Similarity> findAllByEvent1OrEvent2(Long event1, Long event2);

    @Query("""
            SELECT DISTINCT es
            FROM Similarity es
            WHERE es.event1 IN :sourceEventIds OR es.event2 IN :targetEventIds
            """)
    List<Similarity> findAllBySourceEventIdInOrTargetEventIdIn(Set<Long> sourceEventIds, Set<Long> targetEventIds);

    Optional<Similarity> findByEvent1AndEvent2(Long event1, Long event2);
}