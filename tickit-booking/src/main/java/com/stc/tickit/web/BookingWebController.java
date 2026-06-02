package com.stc.tickit.web;

import com.stc.tickit.dto.BookingForm;
import com.stc.tickit.exception.InvalidBookingException;
import com.stc.tickit.exception.ResourceNotFoundException;
import com.stc.tickit.exception.SeatUnavailableException;
import com.stc.tickit.service.BookingService;
import com.stc.tickit.service.EventService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

/** US2 (submit booking) + US3 (cancel) via Thymeleaf forms. */
@Slf4j
@Controller
@RequestMapping("/bookings")
@RequiredArgsConstructor
public class BookingWebController {

    private final BookingService bookingService;
    private final EventService eventService;

    @PostMapping
    public String create(@Valid @ModelAttribute("bookingForm") BookingForm form,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes redirect) {
        if (binding.hasErrors()) {
            model.addAttribute("event", eventService.getEvent(form.getEventId()));
            return "events/book";
        }
        try {
            var booking = bookingService.book(form.toRequest());
            redirect.addFlashAttribute("message",
                    "Booked %d seat(s). Your booking id is %d.".formatted(booking.getQuantity(), booking.getId()));
            redirect.addAttribute("email", form.getCustomerEmail());
            return "redirect:/bookings";
        } catch (SeatUnavailableException | InvalidBookingException | ResourceNotFoundException ex) {
            model.addAttribute("error", ex.getMessage());
            model.addAttribute("event", eventService.getEvent(form.getEventId()));
            return "events/book";
        }
    }

    @GetMapping
    public String myBookings(@RequestParam(required = false) String email, Model model) {
        model.addAttribute("email", email);
        if (email != null && !email.isBlank()) {
            model.addAttribute("bookings", bookingService.bookingsForEmail(email));
        }
        return "bookings/list";
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id,
                        @RequestParam String email,
                        RedirectAttributes redirect) {
        try {
            bookingService.cancel(id);
            redirect.addFlashAttribute("message", "Booking " + id + " cancelled. Seats released.");
        } catch (InvalidBookingException | ResourceNotFoundException ex) {
            redirect.addFlashAttribute("error", ex.getMessage());
        }
        redirect.addAttribute("email", email);
        return "redirect:/bookings";
    }
}
