package com.stc.tickit.dto;

import com.stc.tickit.domain.Event;

import java.time.LocalDateTime;

public record EventResponse(
        Long id,
        String name,
        String venue,
        LocalDateTime eventDate,
        int totalSeats,
        int availableSeats
) {
    public static EventResponse from(Event e) {
        return new EventResponse(e.getId(), e.getName(), e.getVenue(),
                e.getEventDate(), e.getTotalSeats(), e.getAvailableSeats());
    }
}
