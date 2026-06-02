package com.stc.tickit.dto;

import jakarta.validation.constraints.*;

/** Inbound payload for creating a booking (US2), used by both the API and the web form. */
public record BookingRequest(

        @NotNull(message = "eventId is required")
        Long eventId,

        @NotBlank(message = "customer name is required")
        String customerName,

        @NotBlank(message = "email is required")
        @Email(message = "email must be valid")
        String customerEmail,

        @Min(value = 1, message = "must book at least 1 seat")
        @Max(value = 10, message = "cannot book more than 10 seats at once")
        int quantity
) {
}
