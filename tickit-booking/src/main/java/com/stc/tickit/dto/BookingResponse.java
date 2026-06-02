package com.stc.tickit.dto;

import com.stc.tickit.domain.Booking;

import java.time.LocalDateTime;

public record BookingResponse(
        Long id,
        Long eventId,
        String eventName,
        String customerEmail,
        int quantity,
        String status,
        LocalDateTime createdAt
) {
    public static BookingResponse from(Booking b) {
        return new BookingResponse(
                b.getId(),
                b.getEvent().getId(),
                b.getEvent().getName(),
                b.getUser().getEmail(),
                b.getQuantity(),
                b.getStatus().name(),
                b.getCreatedAt());
    }
}
