package com.stc.tickit.service;

import com.stc.tickit.domain.*;
import com.stc.tickit.dto.BookingRequest;
import com.stc.tickit.exception.InvalidBookingException;
import com.stc.tickit.exception.ResourceNotFoundException;
import com.stc.tickit.exception.SeatUnavailableException;
import com.stc.tickit.repository.AppUserRepository;
import com.stc.tickit.repository.BookingRepository;
import com.stc.tickit.repository.EventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final EventRepository eventRepository;
    private final AppUserRepository userRepository;

    /**
     * US2: hold and book tickets.
     * The whole allocation runs in a single transaction. The event row is
     * loaded with a PESSIMISTIC_WRITE lock so concurrent bookings serialize
     * here and cannot drive availableSeats below zero (no overbooking).
     */
    @Transactional
    public Booking book(BookingRequest request) {
        // Lock the event row for the duration of the transaction.
        Event event = eventRepository.findByIdForUpdate(request.eventId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found: " + request.eventId()));

        if (event.getEventDate().isBefore(LocalDateTime.now())) {
            throw new InvalidBookingException("Cannot book a past event: " + event.getName());
        }
        if (request.quantity() > event.getAvailableSeats()) {
            throw new SeatUnavailableException(
                    "Only %d seat(s) left for '%s'".formatted(event.getAvailableSeats(), event.getName()));
        }

        AppUser user = findOrCreateUser(request.customerName(), request.customerEmail());

        event.setAvailableSeats(event.getAvailableSeats() - request.quantity());
        eventRepository.save(event);

        Booking booking = Booking.builder()
                .user(user)
                .event(event)
                .quantity(request.quantity())
                .status(BookingStatus.CONFIRMED)
                .createdAt(LocalDateTime.now())
                .build();

        booking = bookingRepository.save(booking);
        log.info("Booking {} confirmed: {} seat(s) for event {} by {}",
                booking.getId(), request.quantity(), event.getId(), user.getEmail());
        return booking;
    }

    /**
     * US3: cancel a booking before the event date and free up the seats.
     * Runs in an isolated transaction; the event is re-locked while seats
     * are returned so the increment is race-free.
     */
    @Transactional
    public void cancel(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found: " + bookingId));

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new InvalidBookingException("Booking " + bookingId + " is already cancelled");
        }
        if (booking.getEvent().getEventDate().isBefore(LocalDateTime.now())) {
            throw new InvalidBookingException("Cannot cancel after the event date");
        }

        Event event = eventRepository.findByIdForUpdate(booking.getEvent().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Event not found"));
        event.setAvailableSeats(event.getAvailableSeats() + booking.getQuantity());
        eventRepository.save(event);

        booking.setStatus(BookingStatus.CANCELLED);
        bookingRepository.save(booking);
        log.info("Booking {} cancelled; {} seat(s) returned to event {}",
                bookingId, booking.getQuantity(), event.getId());
    }

    @Transactional(readOnly = true)
    public List<Booking> bookingsForEmail(String email) {
        return bookingRepository.findByUserEmailWithDetails(email);
    }

    private AppUser findOrCreateUser(String name, String email) {
        return userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(
                        AppUser.builder().name(name).email(email).build()));
    }
}
