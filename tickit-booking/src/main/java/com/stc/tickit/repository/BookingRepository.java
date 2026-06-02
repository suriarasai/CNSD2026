package com.stc.tickit.repository;

import com.stc.tickit.domain.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    /**
     * Fetch-joins event and user so the returned bookings can be safely mapped
     * to DTOs / rendered in views after the transaction closes
     * (open-in-view is disabled, fetch type is LAZY).
     */
    @Query("""
            select b from Booking b
            join fetch b.event
            join fetch b.user
            where b.user.email = :email
            order by b.createdAt desc
            """)
    List<Booking> findByUserEmailWithDetails(@Param("email") String email);
}
