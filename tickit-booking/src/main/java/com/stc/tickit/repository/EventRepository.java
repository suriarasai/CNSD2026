package com.stc.tickit.repository;

import com.stc.tickit.domain.Event;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface EventRepository extends JpaRepository<Event, Long> {

    /**
     * US1: only upcoming events that still have at least one free seat.
     */
    @Query("""
            select e from Event e
            where e.availableSeats > 0 and e.eventDate > :now
            order by e.eventDate asc
            """)
    List<Event> findBookable(@Param("now") LocalDateTime now);

    /**
     * US2: pessimistic write lock on the row so concurrent booking
     * transactions serialize on seat allocation and cannot overbook.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Event e where e.id = :id")
    Optional<Event> findByIdForUpdate(@Param("id") Long id);
}
