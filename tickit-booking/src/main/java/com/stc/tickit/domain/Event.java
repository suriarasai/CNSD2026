package com.stc.tickit.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * A bookable event (US1). availableSeats is decremented on booking (US2)
 * and incremented on cancellation (US3).
 */
@Entity
@Table(name = "event")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Event {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String venue;

    @Column(name = "event_date", nullable = false)
    private LocalDateTime eventDate;

    @Column(name = "total_seats", nullable = false)
    private int totalSeats;

    @Column(name = "available_seats", nullable = false)
    private int availableSeats;

    /**
     * Optimistic version guards concurrent writes; combined with the
     * pessimistic write lock in the repository it prevents overbooking.
     */
    @Version
    @Column(nullable = false)
    private long version;
}
