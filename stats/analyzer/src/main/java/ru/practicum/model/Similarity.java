package ru.practicum.model;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Builder
@Table(name = "similarity")
public class Similarity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "event1", nullable = false)
    private Long event1;
    @Column(name = "event2", nullable = false)
    private Long event2;
    @Column(name = "similarity_score", nullable = false)
    private Double similarityScore;
    @Column(name = "tmsp", nullable = false)
    private LocalDateTime tmsp;
}