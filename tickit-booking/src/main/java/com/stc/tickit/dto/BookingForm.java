package com.stc.tickit.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Mutable form-backing bean for the Thymeleaf booking page.
 * (Thymeleaf's th:field binding relies on JavaBean getters/setters, so a record
 * is unsuitable here — records are used only for the JSON API.)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class BookingForm {

    @NotNull(message = "eventId is required")
    private Long eventId;

    @NotBlank(message = "customer name is required")
    private String customerName;

    @NotBlank(message = "email is required")
    @Email(message = "email must be valid")
    private String customerEmail;

    @Min(value = 1, message = "must book at least 1 seat")
    @Max(value = 10, message = "cannot book more than 10 seats at once")
    private int quantity = 1;

    public BookingForm(Long eventId) {
        this.eventId = eventId;
    }

    public BookingRequest toRequest() {
        return new BookingRequest(eventId, customerName, customerEmail, quantity);
    }
}
